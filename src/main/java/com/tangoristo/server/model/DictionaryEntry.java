package com.tangoristo.server.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.javierartiles.commons.jadict.model.InfoCode;
import com.javierartiles.commons.jadict.model.KEle;
import com.javierartiles.commons.jadict.model.REle;
import com.javierartiles.commons.jadict.model.Sense;
import com.javierartiles.commons.jadict.model.Trans;
import com.neovisionaries.i18n.LanguageCode;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@Builder
public class DictionaryEntry {
    private int entSeq;
    private List<KEle> kEleList;
    private List<REle> rEleList;
    private List<Sense> senseList;
    private List<Trans> transList;

    /**
     * These deprecated fields are left over from
     * a previous implementation using the rikaikun
     * simplified dictionary. Until the client has fully
     * migrated to use the fields above they should be kept.
     */
    @Deprecated
    private String dictionaryForm;
    @Deprecated
    private String alternateForm;
    @JsonDeserialize(using = PartOfSpeechDeserializer.class)
    @Deprecated
    private List<InfoCode> partOfSpeech;
    @Deprecated
    private Map<LanguageCode, List<String>> definitions;
    @Deprecated
    @Builder.Default private String officialProficiencyLevel = "UNKNOWN";
    @Deprecated
    @Builder.Default private boolean isCommonWord = false;
    @Deprecated
    @Builder.Default private boolean isProperNoun = false;

    @JsonIgnore
    public Set<InfoCode> getMiscInfo() {
        return senseList.stream().flatMap(s -> s.getMisc().stream()).collect(Collectors.toSet());
    }
}
