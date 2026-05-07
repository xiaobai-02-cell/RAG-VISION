package org.example.cvrag.api;

import org.example.cvrag.api.dto.SessionCreateResponse;
import org.example.cvrag.api.dto.SessionTurnsResponse;
import org.example.cvrag.domain.ConversationTurn;
import org.example.cvrag.memory.SessionOverview;
import org.example.cvrag.memory.ShortMemoryService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final ShortMemoryService shortMemoryService;

    public SessionController(ShortMemoryService shortMemoryService) {
        this.shortMemoryService = shortMemoryService;
    }

    @GetMapping("/{userId}")
    public List<SessionOverview> listSessions(
            @PathVariable String userId,
            @RequestParam(value = "limit", defaultValue = "30") int limit
    ) {
        return shortMemoryService.listSessions(userId, Math.max(1, Math.min(200, limit)));
    }

    @GetMapping("/{userId}/{sessionId}/turns")
    public SessionTurnsResponse getSessionTurns(@PathVariable String userId, @PathVariable String sessionId) {
        List<ConversationTurn> turns = shortMemoryService.getSessionTurns(userId, sessionId);
        return new SessionTurnsResponse(userId, sessionId, turns);
    }

    @DeleteMapping("/{userId}/{sessionId}")
    public Map<String, Object> deleteSession(@PathVariable String userId, @PathVariable String sessionId) {
        shortMemoryService.deleteSession(userId, sessionId);
        return Map.of("deleted", true, "userId", userId, "sessionId", sessionId);
    }

    @PostMapping("/new")
    public SessionCreateResponse createSession(@RequestParam(value = "userId", required = false) String userId) {
        String resolvedUserId = (userId == null || userId.isBlank()) ? "u-demo" : userId.trim();
        String sessionId = "s-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 6);
        return new SessionCreateResponse(resolvedUserId, sessionId);
    }
}
