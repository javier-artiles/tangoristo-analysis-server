package com.tangoristo.server.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.net.URL;
import java.util.Date;

@Builder
@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Document {
    private Date crawlDate;
    private Date documentCreationDate;
    private URL url;
    private URL audioStreamUrl;
    private URL videoStreamUrl;
    private String sourceName;
    private String title;
    private VocabularyAnalysisResult titleAnalysis;
    private URL leadingImageUrl;
    private DocumentCategory category;
    private VocabularyLevel vocabularyLevel;
    private double vocabularyLevelScore;
}
