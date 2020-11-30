package com.tangoristo.server.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Entity
@Table(name = "word_form_jpn", indexes = { @Index(columnList = "form") })
public class JapaneseWordForm {

    @Id
    @GeneratedValue
    private int id;

    @Column(name = "form", nullable = false)
    private String form;

}
