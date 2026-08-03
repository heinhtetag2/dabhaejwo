import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 마이그레이션 전 사전 점검. 읽기 전용 — 아무것도 만들거나 바꾸지 않는다.
 *
 * <p><b>왜 필요한가.</b> PostgreSQL 은 DDL 이 트랜잭션이라 마이그레이션이 중간에 실패해도
 * 깨끗하게 롤백된다. 그래도 확인하고 시작하는 편이 낫다 — {@code V1__init.sql} 이
 * {@code CREATE EXTENSION vector} 로 시작하는데, 서버에 pgvector 패키지가 깔려 있지 않거나
 * 계정에 확장 생성 권한이 없으면 거기서 멈춘다. 실패 자체는 안전하지만 원인을 찾는 데
 * 시간이 든다.
 *
 * <p><b>실행</b> (JDK 21 단일 파일 실행, 빌드 불필요):
 * <pre>
 * java --class-path {postgresql.jar} scripts/DbProbe.java \
 *      $POSTGRES_HOST $POSTGRES_PORT $POSTGRES_DB $POSTGRES_USER $POSTGRES_PASSWORD
 * </pre>
 * 드라이버 jar 는 `./gradlew build` 후 Gradle 캐시에 있다.
 */
public final class DbProbe {

    public static void main(String[] args) throws Exception {
        if (args.length < 5) {
            System.err.println("usage: DbProbe <host> <port> <db> <user> <password>");
            System.exit(2);
        }
        String url = "jdbc:postgresql://" + args[0] + ":" + args[1] + "/" + args[2]
                + "?connectTimeout=8";

        boolean ready = true;

        try (Connection c = DriverManager.getConnection(url, args[3], args[4]);
             Statement s = c.createStatement()) {

            System.out.println("연결        : 성공");
            System.out.println("버전        : " + scalar(s, "SELECT version()"));
            System.out.println("DB/계정     : " + scalar(s, "SELECT current_database()")
                    + " / " + scalar(s, "SELECT current_user"));
            System.out.println("superuser   : " + scalar(s,
                    "SELECT usesuper FROM pg_user WHERE usename = current_user"));

            // pgcrypto 는 gen_random_uuid() 때문에 필요하다.
            // PG 13+ 는 코어에 gen_random_uuid() 가 있지만 V1 이 확장을 명시하므로 함께 본다.
            for (String ext : new String[] {"vector", "pgcrypto"}) {
                String installed = scalar(s,
                        "SELECT extversion FROM pg_extension WHERE extname = '" + ext + "'");
                if (installed != null) {
                    System.out.println("확장 " + pad(ext) + ": 설치됨 (" + installed + ")");
                    continue;
                }
                String available = scalar(s,
                        "SELECT default_version FROM pg_available_extensions WHERE name = '" + ext + "'");
                if (available != null) {
                    System.out.println("확장 " + pad(ext) + ": 설치 가능 (" + available
                            + ") — V1 이 CREATE EXTENSION 으로 만든다");
                } else {
                    System.out.println("확장 " + pad(ext) + ": ! 서버에 없다."
                            + " 패키지를 먼저 설치할 것 (예: postgresql-17-pgvector)");
                    ready = false;
                }
            }

            String tables = list(s,
                    "SELECT tablename FROM pg_tables WHERE schemaname = 'public' ORDER BY tablename");
            int count = tables.isEmpty() ? 0 : tables.split(", ").length;
            System.out.println("기존 테이블 : " + count + "개");
            if (count > 0) {
                System.out.println("            " + tables);
                System.out.println("            ! 비어 있지 않다. Flyway 는 baseline-on-migrate=false 이므로"
                        + " 이미 적용된 스키마가 아니면 실패한다");
            }
        }

        System.out.println();
        System.out.println(ready
                ? "마이그레이션을 적용해도 된다."
                : "마이그레이션을 적용하면 안 된다. 위 ! 항목을 먼저 해결할 것.");
        System.exit(ready ? 0 : 1);
    }

    private static String pad(String name) {
        return (name + "       ").substring(0, 8);
    }

    private static String scalar(Statement s, String sql) throws SQLException {
        try (ResultSet r = s.executeQuery(sql)) {
            return r.next() ? r.getString(1) : null;
        }
    }

    private static String list(Statement s, String sql) throws SQLException {
        StringBuilder sb = new StringBuilder();
        try (ResultSet r = s.executeQuery(sql)) {
            while (r.next()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(r.getString(1));
            }
        }
        return sb.toString();
    }
}
