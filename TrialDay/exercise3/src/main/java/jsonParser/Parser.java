package jsonParser;

import com.bitmovin.api.encoding.encodings.Encoding;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Parser {
    public static class JsonParser {
        public static String getKeyFromJson() {
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                String path = new File(".").getCanonicalPath() + "/src/main/java/config.json";
                byte[] jsonData = Files.readAllBytes(Paths.get(path));
                JsonNode rootNode = objectMapper.readTree(jsonData);
                JsonNode key = rootNode.path("key");
                return key.asText();
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(0);
            }
            return null;
        }

        public static void parseAndSaveToFile(List<Encoding> encodings, String path) {
            ObjectMapper mapper = new ObjectMapper();
            ObjectWriter writer = mapper.writer(new DefaultPrettyPrinter());
            try {
                writer.writeValue(new File(path), encodings);
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(0);
            }
        }
    }
}
