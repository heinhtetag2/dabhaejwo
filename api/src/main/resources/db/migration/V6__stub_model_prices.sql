-- STUB 공급사 단가.
--
-- 왜 0원짜리 행을 넣는가. LlmGateway 는 모든 호출에서 model_prices 를 조회해 원가를
-- 확정 저장한다. STUB 만 예외 처리하면 원가 경로에 우회로가 생기고, 그 우회로는 나중에
-- 실 공급사에도 쓰이게 된다 — "모든 LLM 호출은 게이트웨이를 지난다"는 규칙이 그렇게 무너진다.
--
-- stub 호출은 실제로 돈이 들지 않으므로 0원이 거짓이 아니다. ai_usage 에는 정상적으로
-- 행이 쌓이고, 파이프라인이 실제로 도는지 원가 데이터로 확인할 수 있다.
INSERT INTO model_prices (provider, model, purpose_kind, input_per_1m, output_per_1m, effective_from, note) VALUES
  ('STUB', 'stub-generate',  'GENERATE', 0.00, 0.00, '2026-08-01T00:00:00Z', '실제 호출 없음 — 원가 0'),
  ('STUB', 'stub-embedding', 'EMBED',    0.00, NULL, '2026-08-01T00:00:00Z', '실제 호출 없음 — 원가 0');
