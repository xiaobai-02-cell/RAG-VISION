package org.example.cvrag.llm;

import com.fasterxml.jackson.annotation.JsonAlias;
import org.example.cvrag.config.RagProperties;
import org.example.cvrag.domain.ConversationTurn;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
@ConditionalOnProperty(prefix = "rag.model", name = "mock-enabled", havingValue = "false")
public class DashScopeLlmGateway implements LlmGateway {

    private final RagProperties properties;
    private final RestClient restClient;

    public DashScopeLlmGateway(RagProperties properties) {
        this.properties = properties;
        this.restClient = DashScopeClientSupport.create(properties);
    }

    @Override
    public String rewriteQuery(String question, List<ConversationTurn> shortMemory, List<String> longMemoryHints) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是查询改写器。将问题改写为便于检索的独立问句，保留核心约束。\n");
        if (shortMemory != null && !shortMemory.isEmpty()) {
            sb.append("最近会话：\n");
            shortMemory.stream().limit(6).forEach(t -> sb.append(t.role()).append(": ").append(t.content()).append('\n'));
        }
        if (longMemoryHints != null && !longMemoryHints.isEmpty()) {
            sb.append("用户偏好：\n");
            longMemoryHints.forEach(h -> sb.append("- ").append(h).append('\n'));
        }
        sb.append("原问题：").append(question).append("\n改写问句：");
        return chat(sb.toString());
    }

    @Override
    public String summarizeTurns(List<ConversationTurn> oldTurns) {
        StringBuilder sb = new StringBuilder();
        sb.append("请将以下历史对话压缩为短摘要，保留需求、关键参数和结论：\n");
        oldTurns.forEach(t -> sb.append(t.role()).append(": ").append(t.content()).append('\n'));
        return chat(sb.toString());
    }

    @Override
    public String answer(String prompt) {
        return chat(prompt);
    }

    private String chat(String userPrompt) {
        ChatRequest req = new ChatRequest(
                properties.getModel().getChatModel(),
                properties.getModel().getTemperature(),
                List.of(new Message("user", userPrompt))
        );
        ChatResponse resp = restClient.post()
                .uri("/chat/completions")
                .body(req)
                .retrieve()
                .body(ChatResponse.class);
        if (resp == null || resp.choices() == null || resp.choices().isEmpty()) {
            return "模型无返回。";
        }
        Message msg = resp.choices().get(0).message();
        return msg == null ? "模型无返回。" : msg.content();
    }

    private record ChatRequest(
            String model,
            double temperature,
            List<Message> messages
    ) {
    }

    private record ChatResponse(
            @JsonAlias("choices") List<Choice> choices
    ) {
    }

    private record Choice(
            Message message
    ) {
    }

    private record Message(
            String role,
            String content
    ) {
    }
}
