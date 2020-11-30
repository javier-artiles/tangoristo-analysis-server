package com.tangoristo.server.scripts;

import com.javierartiles.commons.jadict.model.KEle;
import com.javierartiles.commons.jadict.model.REle;
import com.tangoristo.server.entity.JapaneseDictionaryEntry;
import com.tangoristo.server.entity.JapaneseWordForm;
import com.tangoristo.server.model.DictionaryEntry;
import com.tangoristo.server.repository.JapaneseDictionaryRepository;
import com.tangoristo.server.services.impl.JapaneseDictionaryLoader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
public class DictionaryDBPopulator {

    private final JapaneseDictionaryRepository japaneseDictionaryRepository;
    private final JapaneseDictionaryLoader japaneseDictionaryLoader;

    @Autowired
    public DictionaryDBPopulator(
            JapaneseDictionaryRepository japaneseDictionaryRepository,
            JapaneseDictionaryLoader japaneseDictionaryLoader
    ) {
        this.japaneseDictionaryRepository = japaneseDictionaryRepository;
        this.japaneseDictionaryLoader = japaneseDictionaryLoader;
    }

    public void run() throws ParserConfigurationException, SAXException, IOException {
        log.info("Storing dictionary entries");
        japaneseDictionaryLoader.loadDictionaryEntries()
                .stream()
                .map(this::asDictionaryEntryEntity)
                .forEach(this::save);
    }

    private void save(JapaneseDictionaryEntry japaneseDictionaryEntry) {
        try {
            japaneseDictionaryRepository.save(japaneseDictionaryEntry);
        } catch (JpaSystemException e) {
            log.warn("Failed to save dictionary entry with ID = {}. {}", japaneseDictionaryEntry.getId(), e.getMessage());
        }
    }

    private JapaneseDictionaryEntry asDictionaryEntryEntity(DictionaryEntry dictionaryEntry) {
        Stream<String> kebStream = dictionaryEntry.getKEleList().stream().map(KEle::getKeb);
        Stream<String> rebStream = dictionaryEntry.getREleList().stream().map(REle::getReb);
        List<JapaneseWordForm> forms = Stream.concat(kebStream, rebStream)
                .map(str -> JapaneseWordForm.builder().form(str).build())
                .collect(Collectors.toList());
        return JapaneseDictionaryEntry.builder()
                .id(dictionaryEntry.getEntSeq())
                .forms(forms)
                .dictionaryEntry(dictionaryEntry)
                .build();
    }

}
