package com.tangoristo.server.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.ToString;
import lombok.Value;

@Value
@AllArgsConstructor
@Builder
@ToString
public class VocabularyEntity {
    private DictionaryEntry dictionaryEntry;
    private List<TokenSequence> tokenSequenceOccurrences;
    private String reading;
    private int firstOccurrenceOffset;
}
