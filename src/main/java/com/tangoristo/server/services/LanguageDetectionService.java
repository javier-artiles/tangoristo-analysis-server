package com.tangoristo.server.services;

import com.neovisionaries.i18n.LanguageCode;

public interface LanguageDetectionService {
    java.util.Optional<LanguageCode> detectLanguage(String text);
}
