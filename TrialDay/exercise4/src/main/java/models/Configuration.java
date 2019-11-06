package models;

public class Configuration {
    private String s3AccessKey;
    private String s3SecretKey;
    private String s3BucketName;
    private String s3Region;
    private String sourceHostUrl;
    private String sourcePathUrl;
    private String bitmovinKey;

    public String getS3BucketName() {
        return s3BucketName;
    }

    public void setS3BucketName(String s3BucketName) {
        this.s3BucketName = s3BucketName;
    }

    public String getS3SecretKey() {
        return s3SecretKey;
    }

    public void setS3SecretKey(String s3SecretKey) {
        this.s3SecretKey = s3SecretKey;
    }

    public String getS3AccessKey() {
        return s3AccessKey;
    }

    public void setS3AccessKey(String s3AccessKey) {
        this.s3AccessKey = s3AccessKey;
    }

    public String getSourceHostUrl() {
        return sourceHostUrl;
    }

    public void setSourceHostUrl(String sourceHostUrl) {
        this.sourceHostUrl = sourceHostUrl;
    }

    public String getBitmovinKey() {
        return bitmovinKey;
    }

    public void setBitmovinKey(String bitmovinKey) {
        this.bitmovinKey = bitmovinKey;
    }

    public String getS3Region() {
        return s3Region;
    }

    public void setS3Region(String s3Region) {
        this.s3Region = s3Region;
    }

    public String getSourcePathUrl() {
        return sourcePathUrl;
    }

    public void setSourcePathUrl(String sourcePathUrl) {
        this.sourcePathUrl = sourcePathUrl;
    }
}
