package org.example.cvrag.api;

import jakarta.validation.Valid;
import org.example.cvrag.api.dto.IngestResponse;
import org.example.cvrag.api.dto.TextIngestRequest;
import org.example.cvrag.ingest.KnowledgeIngestionService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ingest")
public class KnowledgeIngestionController {

    private final KnowledgeIngestionService ingestionService;

    public KnowledgeIngestionController(KnowledgeIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/text")
    public IngestResponse ingestText(@Valid @RequestBody TextIngestRequest request) {
        KnowledgeIngestionService.IngestResult result = ingestionService.ingestText(
                request.docId(),
                request.sourceName(),
                request.content(),
                buildMetadata("text", request.docType(), request.project(), request.tags())
        );
        return new IngestResponse(result.docId(), result.textChunkCount(), result.imageChunkCount(), "text ingested");
    }

    @PostMapping(path = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public IngestResponse ingestFile(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "docId", required = false) String docId,
            @RequestParam(value = "docType", required = false) String docType,
            @RequestParam(value = "project", required = false) String project,
            @RequestParam(value = "tags", required = false) String tags
    ) {
        KnowledgeIngestionService.IngestResult result = ingestionService.ingestFile(
                docId,
                file,
                buildMetadata("file", docType, project, tags)
        );
        return new IngestResponse(result.docId(), result.textChunkCount(), result.imageChunkCount(), "file ingested");
    }

    @PostMapping(path = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public IngestResponse ingestImage(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "docId", required = false) String docId,
            @RequestParam(value = "caption", required = false) String caption,
            @RequestParam(value = "docType", required = false) String docType,
            @RequestParam(value = "project", required = false) String project,
            @RequestParam(value = "tags", required = false) String tags
    ) {
        KnowledgeIngestionService.IngestResult result = ingestionService.ingestImage(
                docId,
                file,
                caption,
                buildMetadata("image", docType, project, tags)
        );
        return new IngestResponse(result.docId(), result.textChunkCount(), result.imageChunkCount(), "image ingested");
    }

    @DeleteMapping("/doc/{docId}")
    public void deleteDocument(@PathVariable String docId) {
        ingestionService.deleteDocument(docId);
    }

    private Map<String, String> buildMetadata(String channel, String docType, String project, String tags) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("channel", channel);
        if (docType != null && !docType.isBlank()) {
            metadata.put("docType", docType.trim());
        }
        if (project != null && !project.isBlank()) {
            metadata.put("project", project.trim());
        }
        if (tags != null && !tags.isBlank()) {
            metadata.put("tags", tags.trim());
        }
        return metadata;
    }
}
