package com.tangoristo.server.model;

import lombok.Value;

import java.util.List;

@Value
public class DictionarySearchResults {

    private int total;
    private int start;
    private List<DictionaryEntry> hits;

}
