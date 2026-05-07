package org.example.cvrag.ingest;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Component
public class TikaDocumentParser {

    private final Tika tika = new Tika();

    public String parse(MultipartFile file) {
        try {
            return tika.parseToString(file.getInputStream());
        } catch (IOException | TikaException e) {
            throw new IllegalStateException("Failed to parse file with Apache Tika: " + file.getOriginalFilename(), e);
        }
    }
}
