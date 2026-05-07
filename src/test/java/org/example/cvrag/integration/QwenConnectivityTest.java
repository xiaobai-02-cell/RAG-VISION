package org.example.cvrag.integration;

import org.example.cvrag.config.RagProperties;
import org.example.cvrag.llm.LlmGateway;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "rag.model.mock-enabled=false",
                "rag.memory.redis-enabled=false"
        }
)
class QwenConnectivityTest {

    @Autowired
    private LlmGateway llmGateway;

    @Autowired
    private RagProperties ragProperties;

    @Test
    void shouldCallQwenChatModelAndPrintOutput() {
        String apiKey = ragProperties.getModel().getApiKey();
        Assertions.assertNotNull(apiKey, "DashScope API key is null");
        Assertions.assertFalse(apiKey.isBlank(), "DashScope API key is blank");

        String answer = Assertions.assertTimeoutPreemptively(
                Duration.ofSeconds(45),
                () -> llmGateway.answer("请只输出一句话：Qwen3.6-plus 连通测试通过。不要输出其他内容。")
        );

        Assertions.assertNotNull(answer, "Qwen response is null");
        Assertions.assertFalse(answer.isBlank(), "Qwen response is blank");
        Assertions.assertFalse(answer.contains("Mock答复"), "Still using MockLlmGateway, mock mode not disabled");
        Assertions.assertTrue(
                answer.contains("连通测试通过"),
                "Qwen response did not contain expected phrase: " + answer
        );

        // Print model output in test logs for direct visibility.
        System.out.println("QWEN_TEST_OUTPUT: " + answer);
    }
}
