package com.tangoristo.server.config;

import com.tangoristo.server.repository.JapaneseDictionaryRepository;
import com.tangoristo.server.scripts.DictionaryDBPopulator;
import com.tangoristo.server.services.DictionaryService;
import com.tangoristo.server.services.impl.DBDictionaryService;
import com.tangoristo.server.services.impl.InMemoryDictionaryService;
import com.tangoristo.server.services.impl.JapaneseDictionaryLoader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class DictionaryServiceConfig {

    private static final String USE_IN_MEMORY_DICTIONARY = "settings.dictionary.inMemory";

    @Bean
    @ConditionalOnProperty(name = USE_IN_MEMORY_DICTIONARY, havingValue = "false")
    public DictionaryService getDBDictionaryService(
            JapaneseDictionaryRepository japaneseDictionaryRepository,
            DictionaryDBPopulator dictionaryDBPopulator
    ) {
        long count = japaneseDictionaryRepository.count();
        log.info("Using DB dictionary service");
        log.info("Found {} entries in the Japanese dictionary", count);
        if (count == 0) {
            log.info("Populating Japanese dictionary");
            try {
                dictionaryDBPopulator.run();
            } catch (Exception e) {
                log.error("Failed to populate the database", e);
            }
        }
        return new DBDictionaryService(japaneseDictionaryRepository);
    }

    @Bean
    @ConditionalOnProperty(name = USE_IN_MEMORY_DICTIONARY, havingValue = "true")
    public DictionaryService getInMemoryDictionaryService(
            JapaneseDictionaryLoader japaneseDictionaryLoader
    ) {
        log.info("Using in memory dictionary service");
        return new InMemoryDictionaryService(japaneseDictionaryLoader);
    }

}
