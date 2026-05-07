package org.example.cvrag.api;

import jakarta.validation.Valid;
import org.example.cvrag.api.dto.ChatRequest;
import org.example.cvrag.api.dto.ChatResponse;
import org.example.cvrag.service.RagOrchestratorService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final RagOrchestratorService ragOrchestratorService;

    public ChatController(RagOrchestratorService ragOrchestratorService) {
        this.ragOrchestratorService = ragOrchestratorService;
    }

    @PostMapping
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        return ragOrchestratorService.chat(request);
    }
}
