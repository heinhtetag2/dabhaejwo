-- 파일 업로드. 원본을 오브젝트 저장소(Cloudflare R2)에 두고 문서 행이 그것을 가리킨다.
--
-- 원본을 DB 에 넣지 않는 이유는 명확하다 — 카탈로그 PDF 하나가 수십 MB 다.
-- DB 는 그 크기를 다루라고 만든 물건이 아니고, 백업과 복제가 그만큼 무거워진다.

-- 저장소 안의 키. 우리가 정하는 값이라 파일명과 무관하다 —
-- 방문자가 올린 파일명을 그대로 키로 쓰면 경로 조작(../)과 충돌이 동시에 생긴다.
-- 형태: tenants/{tenantId}/documents/{documentId}
ALTER TABLE knowledge_documents ADD COLUMN storage_key text;

-- 업로드 당시 판정한 MIME. 다운로드·본문 추출에서 무엇으로 다룰지 정한다.
-- 클라이언트가 보낸 값을 그대로 믿지 않고 서버가 확장자와 대조한 결과를 적는다.
ALTER TABLE knowledge_documents ADD COLUMN content_type text;

-- 사용자가 올린 원래 파일명. 화면에 보여주기 위한 값이며 키로는 쓰지 않는다.
ALTER TABLE knowledge_documents ADD COLUMN original_filename text;

-- 같은 파일을 두 번 올렸는지 판별한다. content_sha256 은 V1 에 이미 있다.
CREATE INDEX ON knowledge_documents (tenant_id, content_sha256);
