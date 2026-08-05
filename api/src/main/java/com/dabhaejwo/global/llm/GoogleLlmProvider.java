package com.dabhaejwo.global.llm;

import com.dabhaejwo.domain.provider.service.ProviderCredentialService;
import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Google Gemini 어댑터.
 *
 * <p>공급사별 편차는 이 클래스 밖으로 새어나가지 않는다 — 서비스는 {@link LlmGateway} 만 안다.
 *
 * <p><b>왜 {@code generateContent} 인가</b> — 문서상 권장은 Interactions API 지만
 * 그쪽은 키 전달 방식이 레퍼런스에 명시돼 있지 않았다. {@code generateContent} 는
 * 요청·응답 형상이 전부 확인되고 아직 동작한다. 어댑터가 한 클래스라 나중에 갈아끼우기 쉽다
 * (docs/IMPROVEMENTS.md 등록).
 *
 * <p>키는 <b>헤더</b>로 보낸다. 쿼리 파라미터로 보내면 접근 로그·프록시 로그에 키가 남는다.
 */
@Component
public class GoogleLlmProvider implements LlmProvider {

    private static final Logger log = LoggerFactory.getLogger(GoogleLlmProvider.class);

    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta";
    private static final String KEY_HEADER = "x-goog-api-key";

    /** knowledge_chunks.embedding 과 일치해야 한다. 공급사에 이 차원으로 달라고 요청한다. */
    private static final int EMBEDDING_DIMENSION = 1536;

    /**
     * 생각(thinking) 토큰 예산. <b>0 = 끔.</b>
     *
     * <p>Gemini 3 계열은 기본으로 생각을 한다. 실측 결과 같은 질문에서
     * 생각 464토큰 / 답변 34토큰이 나왔다 — <b>원가의 대부분이 답이 아니라 생각이다.</b>
     * 게다가 생각 토큰이 {@code maxOutputTokens} 를 함께 먹어, 400 예산에서 생각이 380을
     * 가져가고 답변이 {@code MAX_TOKENS} 로 잘리는 일이 실제로 벌어졌다.
     *
     * <p>끄고 재보니 답변 품질은 같았다(39토큰, 완결). 근거 문서에서 사실을 뽑아내는
     * 작업이라 깊은 추론이 필요 없기 때문이다.
     *
     * <p>중간값(128)은 지켜지지 않았다(347토큰 사용). 이 모델에서는 0 만 확실하다.
     *
     * <p>공급사마다 개념이 달라 설정(cost_guards)이 아니라 어댑터에 둔다.
     * 추론이 필요한 업체가 생기면 그때 설정으로 올린다 (docs/IMPROVEMENTS.md).
     */
    private static final int THINKING_BUDGET = 0;

    /**
     * 공급사가 멈추면 요청 스레드가 물린다. 방문자 질문은 사람이 기다리는 요청이라
     * 오래 붙잡느니 실패하는 편이 낫다.
     */
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(30);

    private final RestClient restClient;
    private final ProviderCredentialService credentialService;

    public GoogleLlmProvider(ProviderCredentialService credentialService) {
        this.credentialService = credentialService;

        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build());
        factory.setReadTimeout(READ_TIMEOUT);

        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .requestFactory(factory)
                .build();
    }

    @Override
    public LlmProviderName name() {
        return LlmProviderName.GOOGLE;
    }

    /** 키가 없으면 게이트웨이가 이 공급사를 고르지 않는다. STUB 으로 조용히 떨어지지 않는다. */
    @Override
    public boolean available() {
        return credentialService.configured(LlmProviderName.GOOGLE);
    }

    @Override
    public GenerateResult generate(GenerateRequest request) {
        Map<String, Object> body = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", request.systemPrompt()))),
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", request.userPrompt())))),
                // 출력 상한이 곧 원가 상한이다. 샘플링 파라미터는 최신 모델에서 무시되므로 보내지 않는다.
                "generationConfig", Map.of(
                        "maxOutputTokens", request.maxOutputTokens(),
                        "thinkingConfig", Map.of("thinkingBudget", THINKING_BUDGET)));

        Map<String, Object> response = post(
                "/models/" + request.model() + ":generateContent", body);

        String text = extractText(response);
        if (text.isBlank()) {
            // 안전 정책 차단이거나 후보가 비었다. 빈 문자열을 답변으로 내보내지 않는다.
            log.warn("Gemini 응답에 텍스트가 없습니다. model={} feedback={}",
                    request.model(), response.get("promptFeedback"));
            throw new BusinessException(ErrorCode.LLM_RESPONSE_BLOCKED);
        }

        Map<String, Object> usage = asMap(response.get("usageMetadata"));
        return new GenerateResult(
                text,
                request.model(),
                intOf(usage, "promptTokenCount"),
                // 생각 토큰도 과금 대상이라 출력에 더한다. 빼면 원가가 실제보다 작게 잡힌다.
                intOf(usage, "candidatesTokenCount") + intOf(usage, "thoughtsTokenCount"));
    }

    /**
     * 임베딩.
     *
     * <p><b>토큰을 먼저 센 다음 임베딩한다.</b> 순서가 중요하다 —
     * {@code batchEmbedContents} 응답에는 {@code usageMetadata} 가 <b>없다</b>(실호출로 확인).
     * 그대로 두면 임베딩 원가가 {@code ai_usage} 에 0원으로 쌓이고, 과거 원가는 다시
     * 만들어낼 수 없으므로 그 구멍은 영구적이다.
     *
     * <p>{@code countTokens} 가 실패하면 임베딩을 <b>하지 않는다</b>. 계량할 수 없으면
     * 돈을 쓰지 않는다 — 순서를 뒤집으면 "이미 썼는데 얼마인지 모르는" 상태가 만들어진다.
     * {@code countTokens} 자체는 과금되지 않는다.
     */
    @Override
    public EmbedResult embed(List<String> texts, String model) {
        int inputTokens = countTokens(texts, model);

        List<Map<String, Object>> requests = texts.stream()
                .map(text -> Map.<String, Object>of(
                        // 배치 항목마다 model 을 요구한다. 경로의 모델과 같아야 한다.
                        "model", "models/" + model,
                        "content", Map.of("parts", List.of(Map.of("text", text))),
                        // 3072 를 잘라 쓰는 대신 처음부터 1536 으로 달라고 한다.
                        // 코사인 거리는 크기에 영향받지 않으므로 재정규화는 필요 없다.
                        "outputDimensionality", EMBEDDING_DIMENSION))
                .toList();

        Map<String, Object> response = post(
                "/models/" + model + ":batchEmbedContents", Map.of("requests", requests));

        List<Map<String, Object>> embeddings = asList(response.get("embeddings"));
        if (embeddings.size() != texts.size()) {
            throw new IllegalStateException(
                    "임베딩 개수가 요청과 다릅니다: " + embeddings.size() + " != " + texts.size());
        }

        List<float[]> vectors = new ArrayList<>(embeddings.size());
        for (Map<String, Object> embedding : embeddings) {
            List<Number> values = asList(embedding.get("values"));
            if (values.size() != EMBEDDING_DIMENSION) {
                // 차원이 다르면 DB 삽입에서 터진다. 여기서 이유를 분명히 하고 멈춘다.
                throw new IllegalStateException(
                        "임베딩 차원이 " + EMBEDDING_DIMENSION + " 이 아닙니다: " + values.size());
            }
            float[] vector = new float[values.size()];
            for (int i = 0; i < values.size(); i++) {
                vector[i] = values.get(i).floatValue();
            }
            vectors.add(vector);
        }

        return new EmbedResult(vectors, model, inputTokens);
    }

    /**
     * 임베딩 대상의 토큰 수. 배치 전체를 한 번에 센다.
     *
     * <p>실패하면 그대로 던진다 — 호출부가 임베딩을 포기하게 만들기 위해서다.
     * 여기서 0 이나 추정치를 돌려주면 원가가 조용히 틀어진다.
     */
    private int countTokens(List<String> texts, String model) {
        List<Map<String, Object>> contents = texts.stream()
                .map(text -> Map.<String, Object>of("parts", List.of(Map.of("text", text))))
                .toList();

        Map<String, Object> response = post(
                "/models/" + model + ":countTokens", Map.of("contents", contents));

        int total = intOf(response, "totalTokens");
        if (total <= 0) {
            // 셌는데 0 이면 응답 형상이 바뀐 것이다. 0원짜리 호출을 원장에 남기지 않는다.
            throw new BusinessException(ErrorCode.LLM_PROVIDER_UNAVAILABLE,
                    "임베딩 토큰 수를 확인하지 못해 중단했습니다");
        }
        return total;
    }

    private Map<String, Object> post(String path, Object body) {
        String apiKey = credentialService.resolveKey(LlmProviderName.GOOGLE);
        if (apiKey == null) {
            throw new BusinessException(ErrorCode.LLM_PROVIDER_UNAVAILABLE,
                    "Google 공급사 키가 등록되어 있지 않습니다");
        }
        try {
            Map<String, Object> response = restClient.post()
                    .uri(path)
                    .header(KEY_HEADER, apiKey)
                    .body(body)
                    .retrieve()
                    .body(MAP_TYPE);
            if (response == null) {
                throw new BusinessException(ErrorCode.LLM_PROVIDER_UNAVAILABLE,
                        "Google 공급사가 빈 응답을 돌려주었습니다");
            }
            return response;
        } catch (BusinessException e) {
            throw e;
        } catch (RestClientException e) {
            // 예외 메시지에 본문이 섞여 키가 로그로 새지 않도록 메시지를 그대로 노출하지 않는다.
            log.error("Google 호출 실패 path={} type={}", path, e.getClass().getSimpleName(), e);
            throw new BusinessException(ErrorCode.LLM_PROVIDER_UNAVAILABLE,
                    "Google 공급사 호출에 실패했습니다");
        }
    }

    @SuppressWarnings("unchecked")
    private String extractText(Map<String, Object> response) {
        List<Map<String, Object>> candidates = asList(response.get("candidates"));
        if (candidates.isEmpty()) {
            return "";
        }
        Map<String, Object> content = asMap(candidates.get(0).get("content"));
        List<Map<String, Object>> parts = asList(content.get("parts"));
        StringBuilder text = new StringBuilder();
        for (Map<String, Object> part : parts) {
            Object value = part.get("text");
            if (value instanceof String string) {
                text.append(string);
            }
        }
        return text.toString().strip();
    }

    private int intOf(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value instanceof Number number ? number.intValue() : 0;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> asList(Object value) {
        return value instanceof List<?> list ? (List<T>) list : List.of();
    }

    private static final org.springframework.core.ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new org.springframework.core.ParameterizedTypeReference<>() {
            };
}
