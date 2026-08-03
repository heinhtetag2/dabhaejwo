import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 마이그레이션 전 사전 점검. 읽기 전용 — 아무것도 만들거나 바꾸지 않는다.
 *
 * <p><b>왜 필요한가.</b> MariaDB 는 DDL 이 트랜잭션이 아니다. V1__init.sql 이 중간에
 * 실패하면 앞쪽 테이블은 만들어진 채 남고 Flyway 는 되돌리지 못한다. 반쯤 만들어진
 * 스키마와 실패한 이력이 함께 남아 손으로 치워야 한다.
 * 그래서 서버 버전과 스키마 상태를 먼저 확인하고 마이그레이션을 건다.
 *
 * <p><b>실행</b> (JDK 21 단일 파일 실행, 빌드 불필요):
 * <pre>
 * java --class-path {mariadb-java-client.jar} scripts/DbProbe.java \
 *      $MARIADB_HOST $MARIADB_PORT $MARIADB_DB $MARIADB_USER $MARIADB_PASSWORD
 * </pre>
 * 드라이버 jar 는 `./gradlew build` 후 Gradle 캐시에 있다.
 */
public final class DbProbe {

    /** MariaDB Vector(VECTOR 타입)가 GA 된 최소 버전. */
    private static final int REQUIRED_MAJOR = 11;
    private static final int REQUIRED_MINOR = 8;

    public static void main(String[] args) throws Exception {
        if (args.length < 5) {
            System.err.println("usage: DbProbe <host> <port> <db> <user> <password>");
            System.exit(2);
        }
        String url = "jdbc:mariadb://" + args[0] + ":" + args[1] + "/" + args[2]
                + "?connectionTimeZone=UTC&connectTimeout=8000";

        boolean ready = true;

        try (Connection c = DriverManager.getConnection(url, args[3], args[4]);
             Statement s = c.createStatement()) {

            System.out.println("연결        : 성공");

            String version = scalar(s, "SELECT VERSION()");
            System.out.println("버전        : " + version);
            System.out.println("스키마/계정 : " + scalar(s, "SELECT DATABASE()")
                    + " / " + scalar(s, "SELECT CURRENT_USER()"));

            if (!versionAtLeast(version)) {
                System.out.println("            ! VECTOR 타입에는 MariaDB "
                        + REQUIRED_MAJOR + "." + REQUIRED_MINOR + " 이상이 필요하다");
                ready = false;
            }

            int tables = Integer.parseInt(scalar(s,
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE()"));
            System.out.println("기존 테이블 : " + tables + "개");
            if (tables > 0) {
                System.out.println("            " + list(s,
                        "SELECT table_name FROM information_schema.tables"
                                + " WHERE table_schema = DATABASE() ORDER BY table_name"));
                System.out.println("            ! 비어 있지 않다. Flyway 는 baseline-on-migrate=false 이므로"
                        + " 이미 적용된 스키마가 아니면 실패한다");
            }

            System.out.println("같은 서버 DB: " + list(s, "SHOW DATABASES"));

            // VECTOR 실제 동작 확인 — 버전 문자열만으로는 배포판 차이를 못 잡는다
            try {
                s.executeQuery("SELECT VEC_DISTANCE_COSINE(VEC_FromText('[1,0]'), VEC_FromText('[0,1]'))");
                System.out.println("VECTOR      : 사용 가능");
            } catch (SQLException e) {
                System.out.println("VECTOR      : 사용 불가 — " + e.getMessage());
                ready = false;
            }
        }

        System.out.println();
        System.out.println(ready
                ? "마이그레이션을 적용해도 된다."
                : "마이그레이션을 적용하면 안 된다. 위 ! 항목을 먼저 해결할 것.");
        System.exit(ready ? 0 : 1);
    }

    private static boolean versionAtLeast(String version) {
        try {
            String[] parts = version.split("[.-]");
            int major = Integer.parseInt(parts[0]);
            int minor = Integer.parseInt(parts[1]);
            return major > REQUIRED_MAJOR || (major == REQUIRED_MAJOR && minor >= REQUIRED_MINOR);
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static String scalar(Statement s, String sql) throws SQLException {
        try (ResultSet r = s.executeQuery(sql)) {
            r.next();
            return r.getString(1);
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
