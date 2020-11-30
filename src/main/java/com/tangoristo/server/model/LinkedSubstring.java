package com.tangoristo.server.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class LinkedSubstring {
    @Builder.Default private Integer vocabularyIndex = null;
    @Builder.Default private Integer sequenceIndex = null;
    private List<String> surfaceList;
    private List<String> readingList;
}
