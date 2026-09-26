package com.codenames.backend.adapter.out.words;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.springframework.stereotype.Component;

import com.codenames.backend.application.port.WordListProvider;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
public class ClasspathWordListProvider implements WordListProvider {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Override
    public List<String> frenchWords() {
        try (InputStream stream = getClass().getResourceAsStream("/words.json")) {
            if (stream == null) {
                throw new IllegalStateException("words.json not found on classpath");
            }
            JsonNode rootNode = jsonMapper.readTree(stream);
            JsonNode frenchWordsNode = rootNode.path("French");
            return jsonMapper.readValue(frenchWordsNode.toString(), new TypeReference<List<String>>() {
            });
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load word list", e);
        }
    }
}
