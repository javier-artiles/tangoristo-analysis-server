package com.tangoristo.server.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

import java.util.List;

@Value
public class AnalysisRequest {
    @JsonProperty("structure_key")
    private String structureKey;
    private String title;
    private String body;
    @JsonProperty("ruby_hint_list")
    private List<RubyHint> rubyHints;
}
