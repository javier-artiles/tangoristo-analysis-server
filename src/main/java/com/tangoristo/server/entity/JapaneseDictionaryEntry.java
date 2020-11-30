package com.tangoristo.server.entity;

import com.tangoristo.server.model.DictionaryEntry;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Entity
@Table(name = "dictionary_jpn")
public class JapaneseDictionaryEntry {

    @Id
    @Column (name = "id", nullable = false)
    private int id;

    @Column(name = "forms")
    @OneToMany(cascade = CascadeType.ALL)
    private List<JapaneseWordForm> forms;

    @Column(name = "dictionary_entry", columnDefinition = "TEXT", nullable = false)
    @Convert(converter = DictionaryEntryConverter.class)
    private DictionaryEntry dictionaryEntry;

}
