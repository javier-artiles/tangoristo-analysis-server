package com.tangoristo.server.config;

import com.optimaize.langdetect.LanguageDetector;
import com.optimaize.langdetect.LanguageDetectorBuilder;
import com.optimaize.langdetect.i18n.LdLocale;
import com.optimaize.langdetect.ngram.NgramExtractors;
import com.optimaize.langdetect.profiles.LanguageProfile;
import com.optimaize.langdetect.profiles.LanguageProfileReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Configuration
@Slf4j
public class LanguageDetectorConfig {
    @Value("${languageDetection}")
    private String[] languageDetection;

    @Bean
    public LanguageDetector getLanguageDetector() {
        List<LanguageProfile> languageProfiles = new ArrayList<>();
        for (String langCode : languageDetection) {
            try {
                languageProfiles.add(new LanguageProfileReader().readBuiltIn(LdLocale.fromString(langCode)));
            } catch(IOException e) {
                log.error("Failed to loadDictionaryEntries language profile for '{}': {}", langCode, e.getMessage());
                throw new RuntimeException(e.getMessage());
            }
        }
        return LanguageDetectorBuilder.create(NgramExtractors.standard())
                .withProfiles(languageProfiles)
                .build();
    }
}
