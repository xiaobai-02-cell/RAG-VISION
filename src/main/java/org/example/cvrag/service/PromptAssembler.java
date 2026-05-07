package org.example.cvrag.service;

import org.example.cvrag.domain.ConversationTurn;
import org.example.cvrag.domain.RetrievedChunk;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PromptAssembler {

    public String assemble(String rewrittenQuery, String originalQuestion, List<ConversationTurn> shortMemory, List<String> longMemoryHints, List<RetrievedChunk> chunks) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个计算机视觉RAG问答助手。\n");
        sb.append("回答必须优先依据检索上下文，不确定时明确说明“不确定”。\n\n");

        if (shortMemory != null && !shortMemory.isEmpty()) {
            sb.append("【短期记忆】\n");
            shortMemory.forEach(turn -> sb.append(turn.role()).append(": ").append(turn.content()).append('\n'));
            sb.append('\n');
        }

        if (longMemoryHints != null && !longMemoryHints.isEmpty()) {
            sb.append("【长期记忆】\n");
            longMemoryHints.forEach(h -> sb.append("- ").append(h).append('\n'));
            sb.append('\n');
        }

        if (chunks != null && !chunks.isEmpty()) {
            sb.append("【RAG检索上下文】\n");
            for (RetrievedChunk chunk : chunks) {
                sb.append("[chunkId=").append(chunk.chunk().id()).append(", modality=").append(chunk.chunk().modality())
                        .append(", score=").append(String.format("%.4f", chunk.finalScore())).append("]\n");
                sb.append(chunk.chunk().content()).append("\n\n");
            }
        }

        sb.append("【改写后的检索问句】\n").append(rewrittenQuery).append("\n\n");
        sb.append("【用户原问题】\n").append(originalQuestion).append("\n\n");
        sb.append("请给出最终答案，并在末尾列出你参考的chunkId列表。");
        return sb.toString();
    }
}
