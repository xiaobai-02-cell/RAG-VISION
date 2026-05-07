package org.example.cvrag.llm;

import org.example.cvrag.domain.ConversationTurn;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(prefix = "rag.model", name = "mock-enabled", havingValue = "true", matchIfMissing = true)
public class MockLlmGateway implements LlmGateway {

    @Override
    public String rewriteQuery(String question, List<ConversationTurn> shortMemory, List<String> longMemoryHints) {
        String contextHint = shortMemory.isEmpty() ? "" : " [ctx]";
        String profileHint = longMemoryHints.isEmpty() ? "" : " [mem]";
        return question.trim() + contextHint + profileHint;
    }

    @Override
    public String summarizeTurns(List<ConversationTurn> oldTurns) {
        if (oldTurns == null || oldTurns.isEmpty()) {
            return "无历史内容。";
        }
        return oldTurns.stream()
                .limit(8)
                .map(t -> "[" + t.createdAt() + "] " + t.role() + ": " + t.content())
                .collect(Collectors.joining(" | ", "历史摘要: ", oldTurns.size() > 8 ? " ..." : ""));
    }

    @Override
    public String answer(String prompt) {
        return "Mock答复：已基于检索上下文处理你的问题。\n\n" + prompt.substring(0, Math.min(prompt.length(), 900));
    }
}
