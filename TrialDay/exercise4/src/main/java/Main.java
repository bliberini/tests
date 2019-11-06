import bitmovin.BitmovinClient;
import jsonParser.Parser;
import models.Configuration;

public class Main {
    public static void main(String[] args) {
        Configuration configuration = Parser.JsonParser.getConfigurationFromFile();
        if (configuration == null) {
            System.out.println("You must place the config.json in /src/main/java with the required configurations:");
            System.exit(0);
        }

        BitmovinClient bitmovinApi = new BitmovinClient(configuration.getBitmovinKey());

        System.out.println("Beginning encoding...");
        bitmovinApi.createEncodingJobToS3(configuration);
        System.out.println("Find your encoding files and manifest in your S3 bucket.");
    }
}
