package com.tangoristo.server.services.impl;

import com.neovisionaries.i18n.LanguageCode;
import com.optimaize.langdetect.LanguageDetector;
import com.optimaize.langdetect.i18n.LdLocale;
import com.optimaize.langdetect.text.CommonTextObjectFactories;
import com.optimaize.langdetect.text.TextObject;
import com.optimaize.langdetect.text.TextObjectFactory;
import com.tangoristo.server.services.LanguageDetectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.base.Optional;

@Service
public class LanguageDetectionServiceImpl implements LanguageDetectionService {
    @Autowired
    private LanguageDetector languageDetector;

    private TextObjectFactory textObjectFactory = CommonTextObjectFactories.forDetectingOnLargeText();

    @Override
    public java.util.Optional<LanguageCode> detectLanguage(String text) {
        TextObject textObject = textObjectFactory.forText(text);
        Optional<LdLocale> lang = languageDetector.detect(textObject);
        return lang.isPresent() ? java.util.Optional.of(LanguageCode.getByCode(lang.get().getLanguage())) : java.util.Optional.empty();
    }
}
