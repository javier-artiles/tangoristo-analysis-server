package com.tangoristo.server.services;

import com.tangoristo.server.model.DictionaryEntry;
import com.tangoristo.server.services.impl.DictionaryServiceException;

import java.util.List;
import java.util.Locale;

public interface DictionaryService {

    List<DictionaryEntry> search(String query, Locale language) throws DictionaryServiceException;

}
