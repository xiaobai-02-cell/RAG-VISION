package org.example.cvrag.api;

import jakarta.validation.Valid;
import org.example.cvrag.api.dto.RememberRequest;
import org.example.cvrag.memory.LongMemoryService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/memory")
public class MemoryController {

    private final LongMemoryService longMemoryService;

    public MemoryController(LongMemoryService longMemoryService) {
        this.longMemoryService = longMemoryService;
    }

    @PostMapping("/remember")
    public Map<String, String> remember(@Valid @RequestBody RememberRequest request) {
        String memoryId = longMemoryService.remember(request.userId(), request.content());
        return Map.of("memoryId", memoryId);
    }
}
