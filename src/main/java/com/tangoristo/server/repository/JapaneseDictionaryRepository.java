package com.tangoristo.server.repository;

import com.tangoristo.server.entity.JapaneseDictionaryEntry;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JapaneseDictionaryRepository extends CrudRepository<JapaneseDictionaryEntry, Integer> {

    long count();

    @Query(value = "SELECT d.id, d.dictionary_entry FROM dictionary_jpn d " +
           "JOIN dictionary_jpn_forms df ON d.id = df.japanese_dictionary_entry_id " +
           "JOIN word_form_jpn f ON f.id = df.forms_id " +
           "WHERE form = :form " +
           "GROUP BY d.id, d.dictionary_entry",
            nativeQuery = true)
    List<JapaneseDictionaryEntry> findByForm(@Param("form") String form);

}
