package com.tangoristo.server.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.tangoristo.server.model.DictionaryEntry;

import javax.persistence.AttributeConverter;
import java.io.IOException;

public class DictionaryEntryConverter implements AttributeConverter<DictionaryEntry, String> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final ObjectReader OBJECT_READER = OBJECT_MAPPER.readerFor(DictionaryEntry.class);
    private static final ObjectWriter OBJECT_WRITER = OBJECT_MAPPER.writerFor(DictionaryEntry.class);

    @Override
    public String convertToDatabaseColumn(DictionaryEntry attribute) {
        try {
            return OBJECT_WRITER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public DictionaryEntry convertToEntityAttribute(String dbData) {
        try {
            return OBJECT_READER.readValue(dbData);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}
