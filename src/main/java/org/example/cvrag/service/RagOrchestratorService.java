package org.example.cvrag.service;

import org.example.cvrag.api.dto.ChatRequest;
import org.example.cvrag.api.dto.ChatResponse;
import org.example.cvrag.domain.ConversationTurn;
import org.example.cvrag.domain.RetrievedChunk;
import org.example.cvrag.llm.LlmGateway;
import org.example.cvrag.memory.LongMemoryService;
import org.example.cvrag.memory.ShortMemoryService;
import org.example.cvrag.retrieval.HybridRetrievalService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RagOrchestratorService {

    private final ShortMemoryService shortMemoryService;
    private final LongMemoryService longMemoryService;
    private final HybridRetrievalService retrievalService;
    private final LlmGateway llmGateway;
    private final PromptAssembler promptAssembler;

    public RagOrchestratorService(
            ShortMemoryService shortMemoryService,
            LongMemoryService longMemoryService,
            HybridRetrievalService retrievalService,
            LlmGateway llmGateway,
            PromptAssembler promptAssembler
    ) {
        this.shortMemoryService = shortMemoryService;
        this.longMemoryService = longMemoryService;
        this.retrievalService = retrievalService;
        this.llmGateway = llmGateway;
        this.promptAssembler = promptAssembler;
    }

    public ChatResponse chat(ChatRequest request) {
        String userId = request.userId();
        String sessionId = request.sessionId();
        String question = request.question();

        List<ConversationTurn> shortMemory = shortMemoryService.loadForPrompt(userId, sessionId);
        List<String> longMemoryHints = longMemoryService.retrieveHints(userId, question, 3);
        String rewrittenQuery = llmGateway.rewriteQuery(question, shortMemory, longMemoryHints);

        List<RetrievedChunk> retrieved = retrievalService.retrieve(rewrittenQuery, request.topK());
        String prompt = promptAssembler.assemble(rewrittenQuery, question, shortMemory, longMemoryHints, retrieved);
        String answer = llmGateway.answer(prompt);

        // Active long-memory write: explicit "记住"/"remember"
        maybePersistLongMemory(userId, question);
        shortMemoryService.appendUserAndAssistant(userId, sessionId, question, answer);

        List<String> chunkIds = retrieved.stream().map(r -> r.chunk().id()).toList();
        return new ChatResponse(answer, rewrittenQuery, chunkIds, longMemoryHints);
    }

    private void maybePersistLongMemory(String userId, String question) {
        String q = question.trim();
        if (q.startsWith("记住") || q.toLowerCase().startsWith("remember")) {
            longMemoryService.remember(userId, q);
        }
    }
}
