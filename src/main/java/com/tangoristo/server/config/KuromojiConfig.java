package com.tangoristo.server.config;

import com.atilika.kuromoji.ipadic.Tokenizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KuromojiConfig {
    @Bean
    public Tokenizer getKuromoji(){
        return new Tokenizer();
    }
}
