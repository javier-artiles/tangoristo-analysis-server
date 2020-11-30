package com.tangoristo.server.model;

import java.util.List;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Value;

@Data
@Builder
public class TokenSequence {
    private List<Token> tokenList;
    private InflectionAnalysisResult inflectionAnalysisResult;

    public boolean isInflected() {
        return inflectionAnalysisResult != null;
    }

    public String getBaseForm() {
        return getTokenList().stream().map(Token::getBaseForm).collect(Collectors.joining(""));
    }

    public String getSurfaceForm() {
        return getTokenList().stream().map(Token::getSurfaceForm).collect(Collectors.joining(""));
    }

    public String getSurfaceReading() {
        return getTokenList().stream().map(Token::getSurfaceReading).collect(Collectors.joining(""));
    }

    public int getFirstOccurrenceOffset() {
        return tokenList.stream().findFirst().get().getStartOffset();
    }
}
