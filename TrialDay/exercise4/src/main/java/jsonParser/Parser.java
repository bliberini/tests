package jsonParser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import models.Configuration;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class Parser {
    public static class JsonParser {
        public static Configuration getConfigurationFromFile() {
            ObjectMapper mapper = new ObjectMapper();
            try {
                String fullPath = new File(".").getCanonicalPath() + "/src/main/java/config.json";
                return mapper.readValue(new FileReader(fullPath), Configuration.class);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                return null;
            } catch (IOException e) {
                return null;
            }
        }
    }
}
