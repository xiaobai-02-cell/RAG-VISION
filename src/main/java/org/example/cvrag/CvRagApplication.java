package org.example.cvrag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CvRagApplication {

    public static void main(String[] args) {
        SpringApplication.run(CvRagApplication.class, args);
    }
}
