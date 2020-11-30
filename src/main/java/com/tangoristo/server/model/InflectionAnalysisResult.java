package com.tangoristo.server.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class InflectionAnalysisResult {
    private String inflectionForm;
    private String inflectionBase;
    private String inflectionName;
}
