package com.tangoristo.server.services.impl;

import com.javierartiles.commons.jadict.model.KEle;
import com.javierartiles.commons.jadict.model.REle;
import com.tangoristo.server.model.DictionaryEntry;
import com.tangoristo.server.services.DictionaryService;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.xml.sax.SAXException;

import javax.annotation.PostConstruct;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public class InMemoryDictionaryService implements DictionaryService {

    private final JapaneseDictionaryLoader japaneseDictionaryLoader;
    private final PatriciaTrie<List<DictionaryEntry>> dictionaryTrie;

    public InMemoryDictionaryService(
            JapaneseDictionaryLoader japaneseDictionaryLoader
    ) {
        this.japaneseDictionaryLoader = japaneseDictionaryLoader;
        this.dictionaryTrie = new PatriciaTrie<>();
    }

    @PostConstruct
    public void init() throws IOException, SAXException, ParserConfigurationException {
        List<DictionaryEntry> dictionaryEntries = japaneseDictionaryLoader.loadDictionaryEntries();
        dictionaryEntries.forEach(dictionaryEntry -> {
            Stream<String> kebStream = dictionaryEntry.getKEleList().stream().map(KEle::getKeb);
            Stream<String> rebStream = dictionaryEntry.getREleList().stream().map(REle::getReb);
            Stream.concat(kebStream, rebStream).forEach(str -> putInTrieOfLists(str, dictionaryEntry, dictionaryTrie));
        });
    }

    @Override
    public List<DictionaryEntry> search(String query, Locale language) throws DictionaryServiceException {
        if (language.equals(Locale.JAPANESE)) {
            return dictionaryTrie.get(query);
        } else {
            throw new DictionaryServiceException("Unsupported language = " + language.getLanguage());
        }
    }

    private static <T> void putInTrieOfLists(String key, T entry, PatriciaTrie<List<T>> trie) {
        List<T> entrySet = trie.getOrDefault(key, new ArrayList<>());
        entrySet.add(entry);
        trie.putIfAbsent(key, entrySet);
    }

}
