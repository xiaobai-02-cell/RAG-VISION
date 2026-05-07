package org.example.cvrag.llm;

import org.example.cvrag.domain.ConversationTurn;

import java.util.List;

public interface LlmGateway {

    String rewriteQuery(String question, List<ConversationTurn> shortMemory, List<String> longMemoryHints);

    String summarizeTurns(List<ConversationTurn> oldTurns);

    String answer(String prompt);
}
