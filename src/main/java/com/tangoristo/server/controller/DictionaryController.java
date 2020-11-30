package com.tangoristo.server.controller;

import com.tangoristo.server.model.DictionaryEntry;
import com.tangoristo.server.model.DictionarySearchResults;
import com.tangoristo.server.services.DictionaryService;
import com.tangoristo.server.services.impl.DictionaryServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@RestController
@Slf4j
public class DictionaryController {

    private final DictionaryService dictionaryService;

    @Autowired
    public DictionaryController(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }


    @RequestMapping(value = "/api/search_dictionary", method = {RequestMethod.GET}, produces = "application/json")
    public ResponseEntity<?> searchDocuments(
            @RequestParam(required = false, defaultValue = "0") int start,
            @RequestParam(required = false, defaultValue = "10") int maxNum,
            @RequestParam(required = false, defaultValue = "*") String query,
            @RequestParam(required = false, defaultValue = "ja") Locale language
            ) {
        List<DictionaryEntry> totalSearchResults = Collections.emptyList();
        try {
            totalSearchResults = dictionaryService.search(query, language);
        } catch (DictionaryServiceException e) {
            log.warn("Failed to search dictionary with query = {}, language = {}", query, language, e);
        }
        int total = totalSearchResults.size();
        List<DictionaryEntry> filteredSearchResults = totalSearchResults.stream()
                .skip(start).limit(maxNum).collect(Collectors.toList());
        DictionarySearchResults dictionarySearchResults = new DictionarySearchResults(total, start, filteredSearchResults);
        return new ResponseEntity<>(dictionarySearchResults, HttpStatus.OK);
    }

}
