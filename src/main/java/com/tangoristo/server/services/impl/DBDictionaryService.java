package com.tangoristo.server.services.impl;

import com.tangoristo.server.entity.JapaneseDictionaryEntry;
import com.tangoristo.server.model.DictionaryEntry;
import com.tangoristo.server.repository.JapaneseDictionaryRepository;
import com.tangoristo.server.services.DictionaryService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Slf4j
public class DBDictionaryService implements DictionaryService {

    private final JapaneseDictionaryRepository dictionaryRepository;

    public DBDictionaryService(
            JapaneseDictionaryRepository dictionaryRepository
    ) {
        this.dictionaryRepository = dictionaryRepository;
    }

    @Override
    public List<DictionaryEntry> search(String query, Locale language) throws DictionaryServiceException {
        if (language.equals(Locale.JAPANESE)) {
            return dictionaryRepository.findByForm(query).stream()
                    .map(JapaneseDictionaryEntry::getDictionaryEntry)
                    .collect(Collectors.toList());
        } else {
            throw new DictionaryServiceException("Unsupported language = " + language.getLanguage());
        }
    }

}
