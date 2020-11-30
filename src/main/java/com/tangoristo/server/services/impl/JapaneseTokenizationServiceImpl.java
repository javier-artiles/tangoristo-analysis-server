package com.tangoristo.server.services.impl;

import com.atilika.kuromoji.ipadic.Token;
import com.atilika.kuromoji.ipadic.Tokenizer;
import com.mariten.kanatools.KanaConverter;
import com.tangoristo.server.services.TokenizationService;
import org.apache.tomcat.util.buf.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class JapaneseTokenizationServiceImpl implements TokenizationService {

    @Autowired
    private Tokenizer kuromoji;

    @Override
    public List<com.tangoristo.server.model.Token> tokenize(String text) {
        return kuromoji.tokenize(text).stream().map(this::asGenericToken).collect(Collectors.toList());
    }

    private com.tangoristo.server.model.Token asGenericToken(Token kuromojiToken) {
        String surfaceReading = kuromojiToken.getReading() != null ?
                KanaConverter.convertKana(kuromojiToken.getReading(), KanaConverter.OP_ZEN_KATA_TO_ZEN_HIRA) : "";
        List<String> partOfSpeechList = Arrays.asList(
                kuromojiToken.getPartOfSpeechLevel1(),
                kuromojiToken.getPartOfSpeechLevel2(),
                kuromojiToken.getPartOfSpeechLevel3(),
                kuromojiToken.getPartOfSpeechLevel4()
        );
        String partOfSpeech = StringUtils.join(partOfSpeechList, ',');
        return com.tangoristo.server.model.Token.builder()
                .baseForm(kuromojiToken.getBaseForm())
                .surfaceForm(kuromojiToken.getSurface())
                .surfaceReading(surfaceReading)
                .partOfSpeech(partOfSpeech)
                .startOffset(kuromojiToken.getPosition())
                .isInflected(false)
                .build();
    }
}
