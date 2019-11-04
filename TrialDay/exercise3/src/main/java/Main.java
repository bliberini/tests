import com.bitmovin.api.BitmovinApi;
import com.bitmovin.api.encoding.encodings.Encoding;
import com.bitmovin.api.exceptions.BitmovinApiException;
import com.bitmovin.api.http.RestException;
import com.mashape.unirest.http.exceptions.UnirestException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import jsonParser.Parser.JsonParser;

public class Main {
    public Main() {
    }

    public static void main(String[] args) {
        BitmovinApi bitmovinApi = null;

        try {
            bitmovinApi = new BitmovinApi(JsonParser.getKeyFromJson());
        } catch (IOException var4) {
            var4.printStackTrace();
        }

        List<Encoding> encodings = new ArrayList();

        try {
            System.out.println("Fetching 200 encodings.");

            while(encodings.size() < 200) {
                encodings.addAll(bitmovinApi.encoding.getAllEncodings(100, encodings.size()));
                System.out.println(String.format("Fetched %s/%s", encodings.size(), 200));
            }
        } catch (URISyntaxException | IOException | RestException | UnirestException | BitmovinApiException var5) {
            var5.printStackTrace();
            System.exit(0);
        }

        System.out.println("Parsing to JSON and storing...");
        JsonParser.parseAndSaveToFile(encodings, "encodings.json");
        System.out.println("Done");
    }
}