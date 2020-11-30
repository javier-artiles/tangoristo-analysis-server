package com.tangoristo.server.controller;

import com.tangoristo.server.model.VocabularyAnalysisResult;
import com.tangoristo.server.services.TextAnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class VocabularyAnalyzerController {

    private final TextAnalysisService textAnalysisService;

    @Autowired
    public VocabularyAnalyzerController(TextAnalysisService textAnalysisService) {
        this.textAnalysisService = textAnalysisService;
    }

    @RequestMapping(value = "/api/analyze_vocabulary_from_text", method = {RequestMethod.POST}, produces = "application/json")
    public ResponseEntity<VocabularyAnalysisResult> analyzeVocabularyFromText(@RequestBody String text) {
        log.info("analyzeVocabularyFromText '{}'", text);
        VocabularyAnalysisResult analysisResult = textAnalysisService.analyze(text);
        return new ResponseEntity<>(analysisResult, HttpStatus.OK);
    }

}
