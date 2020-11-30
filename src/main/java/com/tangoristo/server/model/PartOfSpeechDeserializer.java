package com.tangoristo.server.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.javierartiles.commons.jadict.model.InfoCode;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class PartOfSpeechDeserializer extends JsonDeserializer<List<InfoCode>> {

    private final Map<String, InfoCode> legacyPosToCode;

    public PartOfSpeechDeserializer() {
        legacyPosToCode = new HashMap<>();
        legacyPosToCode.put("int", InfoCode.inter);
        legacyPosToCode.put("p", InfoCode.place);
        legacyPosToCode.put("s", InfoCode.surname);
        legacyPosToCode.put("f", InfoCode.given);
        legacyPosToCode.put("m", InfoCode.given);
        legacyPosToCode.put("st", InfoCode.station);
        legacyPosToCode.put("c", InfoCode.company);
        legacyPosToCode.put("u", InfoCode.unclass);
        legacyPosToCode.put("h", InfoCode.person);
        legacyPosToCode.put("g", InfoCode.unclass);
    }

    @Override
    public List<InfoCode> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        List<InfoCode> infoCodes = new ArrayList<>();
        for (JsonNode infoCodeNode : node) {
            String infoCodeNodeText = infoCodeNode.asText().replace("-", "_");
            for (String infoCodeString : infoCodeNodeText.split(",")) {
                infoCodeString = infoCodeString.trim();
                if (legacyPosToCode.containsKey(infoCodeString)) {
                    infoCodes.add(legacyPosToCode.get(infoCodeString));
                } else {
                    try {
                        infoCodes.add(InfoCode.valueOf(infoCodeString));
                    } catch (Exception e) {
                        log.warn("Failed to deserialize InfoCode from pos string '{}'", infoCodeString);
                    }
                }
            }
        }
        return infoCodes;
    }
}
