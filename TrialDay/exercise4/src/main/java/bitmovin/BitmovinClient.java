package bitmovin;

import com.bitmovin.api.BitmovinApi;
import com.bitmovin.api.encoding.AclEntry;
import com.bitmovin.api.encoding.AclPermission;
import com.bitmovin.api.encoding.EncodingOutput;
import com.bitmovin.api.encoding.InputStream;
import com.bitmovin.api.encoding.codecConfigurations.AACAudioConfig;
import com.bitmovin.api.encoding.codecConfigurations.H264VideoConfiguration;
import com.bitmovin.api.encoding.codecConfigurations.VideoConfiguration;
import com.bitmovin.api.encoding.codecConfigurations.enums.ProfileH264;
import com.bitmovin.api.encoding.encodings.Encoding;
import com.bitmovin.api.encoding.encodings.muxing.FMP4Muxing;
import com.bitmovin.api.encoding.encodings.muxing.MuxingStream;
import com.bitmovin.api.encoding.encodings.muxing.TSMuxing;
import com.bitmovin.api.encoding.encodings.streams.Stream;
import com.bitmovin.api.encoding.enums.CloudRegion;
import com.bitmovin.api.encoding.enums.DashMuxingType;
import com.bitmovin.api.encoding.enums.StreamSelectionMode;
import com.bitmovin.api.encoding.inputs.HttpsInput;
import com.bitmovin.api.encoding.manifest.dash.*;
import com.bitmovin.api.encoding.manifest.hls.HlsManifest;
import com.bitmovin.api.encoding.manifest.hls.MediaInfo;
import com.bitmovin.api.encoding.manifest.hls.MediaInfoType;
import com.bitmovin.api.encoding.manifest.hls.StreamInfo;
import com.bitmovin.api.encoding.outputs.Output;
import com.bitmovin.api.encoding.outputs.S3Output;
import com.bitmovin.api.encoding.status.Task;
import com.bitmovin.api.enums.Status;
import com.bitmovin.api.exceptions.BitmovinApiException;
import com.bitmovin.api.http.RestException;
import com.bitmovin.api.sdk.model.Fmp4Muxing;
import com.mashape.unirest.http.exceptions.UnirestException;
import models.Configuration;
import org.junit.Assert;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;

public class BitmovinClient {
    static String outFolder = "out/";
    private BitmovinApi bitmovinApi;
    private Encoding encoding;

    public BitmovinClient(String bitmovinKey)
    {
        try {
            this.bitmovinApi = new BitmovinApi(bitmovinKey);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    public void createEncodingJobToS3(Configuration config) {
        try {
            this.encoding = this.createEncoding("TrialDay encoding", CloudRegion.valueOf(config.getS3Region()));

            HttpsInput input = this.createHttpsInput(config.getSourceHostUrl());

            S3Output output = this.createS3Output(config.getS3AccessKey(), config.getS3SecretKey(), config.getS3BucketName());

            AACAudioConfig aacConfiguration = this.createAACAudioConfig(128000L, 48000f);

            List<H264VideoConfiguration> videoConfigurations = Arrays.asList(
                    this.createH264VideoConfiguration(240, 400000L),
                    this.createH264VideoConfiguration(360, 800000L),
                    this.createH264VideoConfiguration(480, 1200000L),
                    this.createH264VideoConfiguration(720, 2400000L),
                    this.createH264VideoConfiguration(1080, 4800000L)
            );

            InputStream inputStreamAudio = this.createInputStream(config.getSourcePathUrl(), input);
            InputStream inputStreamVideo = this.createInputStream(config.getSourcePathUrl(), input);

            Stream audioStream = this.createStream(aacConfiguration.getId(), Collections.singleton(inputStreamAudio));

            List<FMP4Muxing> fMp4Muxings = new LinkedList<>(Arrays.asList(
                    this.createFMp4Muxing(audioStream, output, outFolder + "/audio/128kbps_dash", AclPermission.PUBLIC_READ)
            ));
            List<TSMuxing> tSMuxings = new LinkedList<>(Arrays.asList(
                    this.createTSMuxing(audioStream, output, outFolder + "/audio/128kbps_hls", AclPermission.PUBLIC_READ)
            ));

            for (H264VideoConfiguration videoConfiguration : videoConfigurations) {
                Stream stream = this.createStream(videoConfiguration.getId(), Collections.singleton(inputStreamVideo));
                fMp4Muxings.add(this.createFMp4Muxing(stream, output, outFolder + String.format("/video/%sp_dash", videoConfiguration.getHeight()), AclPermission.PUBLIC_READ));
                tSMuxings.add(this.createTSMuxing(stream, output, outFolder + String.format("/video/%sp_hls", videoConfiguration.getHeight()), AclPermission.PUBLIC_READ));
            }

            EncodingOutput encodingOutput = new EncodingOutput();
            encodingOutput.setOutputId(output.getId());
            encodingOutput.setOutputPath(this.outFolder);

            bitmovinApi.encoding.start(this.encoding);

            Task status = bitmovinApi.encoding.getStatus(this.encoding);
            while (status.getStatus() != Status.FINISHED && status.getStatus() != Status.ERROR)
            {
                System.out.println("Encoding...");
                status = bitmovinApi.encoding.getStatus(this.encoding);
                Thread.sleep(2500);
            }

            if (status.getStatus() != Status.FINISHED)
            {
                System.out.println("Encoding finished with error: can not create manifest");
                return;
            }

            System.out.println("Finished encoding. Creating DASH manifest...");

            EncodingOutput manifestDestination = new EncodingOutput();
            manifestDestination.setOutputId(output.getId());
            manifestDestination.setOutputPath(this.outFolder);
            manifestDestination.setAcl(Collections.singletonList(new AclEntry(AclPermission.PUBLIC_READ)));

            DashManifest manifest = this.createDashManifest("manifest.mpd", manifestDestination);
            Period period = this.addPeriodToDashManifest(manifest);
            VideoAdaptationSet videoAdaptationSet = this.addVideoAdaptationSetToPeriod(manifest, period);
            AudioAdaptationSet audioAdaptationSet = this.addAudioAdaptationSetToPeriodWithRoles(manifest, period, "en");

            this.addDashRepresentationToAdaptationSet(DashMuxingType.TEMPLATE, fMp4Muxings.get(0).getId(), "audio/128kbps_dash", manifest, period, audioAdaptationSet);
            for (int i = 1; i < fMp4Muxings.size(); i++) {
                int height = videoConfigurations.get(i - 1).getHeight();
                this.addDashRepresentationToAdaptationSet(DashMuxingType.TEMPLATE, fMp4Muxings.get(i).getId(), String.format("video/%sp_dash", height), manifest, period, videoAdaptationSet);
            }

            bitmovinApi.manifest.dash.startGeneration(manifest);
            Status dashStatus = bitmovinApi.manifest.dash.getGenerationStatus(manifest);
            while (dashStatus != Status.FINISHED && dashStatus != Status.ERROR)
            {
                dashStatus = bitmovinApi.manifest.dash.getGenerationStatus(manifest);
                Thread.sleep(2500);
            }
            if (dashStatus != Status.FINISHED)
            {
                System.out.println("Could not create DASH manifest");
                return;
            }
            System.out.println("Creating HLS manifest");

            HlsManifest manifestHls = this.createHlsManifest("manifest.m3u8", manifestDestination);

            MediaInfo audioMediaInfo = new MediaInfo();
            audioMediaInfo.setName("audio.m3u8");
            audioMediaInfo.setUri("audio.m3u8");
            audioMediaInfo.setGroupId("audio");
            audioMediaInfo.setType(MediaInfoType.AUDIO);
            audioMediaInfo.setEncodingId(this.encoding.getId());
            audioMediaInfo.setStreamId(audioStream.getId());
            audioMediaInfo.setMuxingId(tSMuxings.get(0).getId());
            audioMediaInfo.setLanguage("en");
            audioMediaInfo.setAssocLanguage("en");
            audioMediaInfo.setAutoselect(false);
            audioMediaInfo.setIsDefault(false);
            audioMediaInfo.setForced(false);
            audioMediaInfo.setSegmentPath("audio/128kbps_hls");
            bitmovinApi.manifest.hls.createMediaInfo(manifestHls, audioMediaInfo);

            for (int i = 1; i < tSMuxings.size(); i++) {
                int height = videoConfigurations.get(i - 1).getHeight();
                this.addStreamInfoToHlsManifest(String.format("video_%sp.m3u8", height), videoConfigurations.get(i - 1).getId(), tSMuxings.get(i).getId(), "audio",String.format("video/%sp_hls", height), manifestHls);
            }

            bitmovinApi.manifest.hls.startGeneration(manifestHls);
            Status hlsStatus = bitmovinApi.manifest.hls.getGenerationStatus(manifestHls);
            while (hlsStatus != Status.FINISHED && hlsStatus != Status.ERROR)
            {
                hlsStatus = bitmovinApi.manifest.hls.getGenerationStatus(manifestHls);
                Thread.sleep(2500);
            }
            if (hlsStatus != Status.FINISHED)
            {
                System.out.println("Could not create HLS manifest");
                return;
            }
            System.out.println("Encoding completed successfully");

        } catch (BitmovinApiException | UnirestException | IOException | URISyntaxException | RestException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private Encoding createEncoding(String name, CloudRegion region) throws URISyntaxException, BitmovinApiException, UnirestException, IOException {
        this.encoding = new Encoding();
        this.encoding.setName(name);
        this.encoding.setCloudRegion(region);
        return bitmovinApi.encoding.create(this.encoding);
    }

    private HttpsInput createHttpsInput(String hostUrl) throws URISyntaxException, BitmovinApiException, UnirestException, IOException {
        HttpsInput input = new HttpsInput();
        input.setHost(hostUrl);
        return bitmovinApi.input.https.create(input);
    }

    private S3Output createS3Output(String accessKey, String secretKey, String bucketname) throws URISyntaxException, BitmovinApiException, UnirestException, IOException {
        S3Output output = new S3Output();
        output.setAccessKey(accessKey);
        output.setSecretKey(secretKey);
        output.setBucketName(bucketname);
        return bitmovinApi.output.s3.create(output);
    }

    private AACAudioConfig createAACAudioConfig(long bitrate, float rate) throws URISyntaxException, BitmovinApiException, UnirestException, IOException {
        AACAudioConfig aacConfiguration = new AACAudioConfig();
        aacConfiguration.setBitrate(bitrate);
        aacConfiguration.setRate(rate);
        return bitmovinApi.configuration.audioAAC.create(aacConfiguration);
    }

    private InputStream createInputStream(String inputPath, HttpsInput input) {
        InputStream inputStreamVideo = new InputStream();
        inputStreamVideo.setInputPath(inputPath);
        inputStreamVideo.setInputId(input.getId());
        inputStreamVideo.setSelectionMode(StreamSelectionMode.AUTO);
        inputStreamVideo.setPosition(0);
        return inputStreamVideo;
    }

    private Stream createStream(String codecConfigId, Set<InputStream> inputStreams) throws URISyntaxException, BitmovinApiException, RestException, UnirestException, IOException {
        Stream stream = new Stream();
        stream.setCodecConfigId(codecConfigId);
        stream.setInputStreams(inputStreams);
        return bitmovinApi.encoding.stream.addStream(this.encoding, stream);
    }

    private FMP4Muxing createFMp4Muxing(Stream stream, Output output, String outputPath, AclPermission defaultAclPermission) throws URISyntaxException, BitmovinApiException, RestException, UnirestException, IOException {
        EncodingOutput encodingOutput = this.createEncodingOutput(output, outputPath, defaultAclPermission);
        FMP4Muxing muxing = new FMP4Muxing();
        muxing.addOutput(encodingOutput);
        MuxingStream list = new MuxingStream();
        list.setStreamId(stream.getId());
        muxing.addStream(list);
        muxing.setSegmentLength(4.0);
        return bitmovinApi.encoding.muxing.addFmp4MuxingToEncoding(this.encoding, muxing);
    }

    private TSMuxing createTSMuxing(Stream stream, Output output, String outputPath, AclPermission defaultAclPermission)
            throws URISyntaxException, BitmovinApiException, RestException, UnirestException, IOException
    {
        EncodingOutput encodingOutput = this.createEncodingOutput(output, outputPath, defaultAclPermission);
        TSMuxing muxing = new TSMuxing();
        muxing.addOutput(encodingOutput);
        MuxingStream list = new MuxingStream();
        list.setStreamId(stream.getId());
        muxing.addStream(list);
        muxing.setSegmentLength(4.0);
        return bitmovinApi.encoding.muxing.addTSMuxingToEncoding(this.encoding, muxing);
    }

    private H264VideoConfiguration createH264VideoConfiguration(int height, long bitrate) throws BitmovinApiException, UnirestException, IOException, URISyntaxException {
        H264VideoConfiguration videoConfiguration = new H264VideoConfiguration();
        videoConfiguration.setHeight(height);
        videoConfiguration.setBitrate(bitrate);
        videoConfiguration.setProfile(ProfileH264.HIGH);
        return bitmovinApi.configuration.videoH264.create(videoConfiguration);
    }

    private EncodingOutput createEncodingOutput(Output output, String outputPath, AclPermission defaultAclPermission)
    {
        EncodingOutput encodingOutput = new EncodingOutput();
        encodingOutput.setOutputPath(outputPath);
        encodingOutput.setOutputId(output.getId());

        if (output.getAcl() != null && output.getAcl().size() > 0)
        {
            encodingOutput.setAcl(output.getAcl());
        }
        else
        {
            ArrayList<AclEntry> aclEntries = new ArrayList<>();
            aclEntries.add(new AclEntry(defaultAclPermission));
            encodingOutput.setAcl(aclEntries);
        }

        return encodingOutput;
    }

    private StreamInfo addStreamInfoToHlsManifest(String uri, String streamId, String muxingId,
                                                  String audioGroupId, String segmentPath, HlsManifest manifest) throws URISyntaxException, BitmovinApiException, RestException, UnirestException, IOException
    {
        StreamInfo s = new StreamInfo();
        s.setUri(uri);
        s.setEncodingId(this.encoding.getId());
        s.setStreamId(streamId);
        s.setMuxingId(muxingId);
        s.setAudio(audioGroupId);
        s.setSegmentPath(segmentPath);
        return bitmovinApi.manifest.hls.createStreamInfo(manifest, s);
    }


    private HlsManifest createHlsManifest(String name, EncodingOutput output) throws URISyntaxException, BitmovinApiException, UnirestException, IOException
    {
        HlsManifest m = new HlsManifest();
        m.setName(name);
        m.addOutput(output);
        return bitmovinApi.manifest.hls.create(m);
    }

    private void addDashRepresentationToAdaptationSet(DashMuxingType type, String muxingId,
                                                      String segmentPath, DashManifest manifest, Period period,
                                                      AdaptationSet adaptationSet) throws BitmovinApiException, URISyntaxException, RestException, UnirestException, IOException
    {
        DashFmp4Representation r = new DashFmp4Representation();
        r.setType(type);
        r.setEncodingId(this.encoding.getId());
        r.setMuxingId(muxingId);
        r.setSegmentPath(segmentPath);
        bitmovinApi.manifest.dash.addRepresentationToAdaptationSet(manifest, period, adaptationSet, r);
    }

    private AudioAdaptationSet addAudioAdaptationSetToPeriodWithRoles(DashManifest manifest, Period period, String lang) throws URISyntaxException, BitmovinApiException, RestException, UnirestException, IOException
    {
        AudioAdaptationSet a = new AudioAdaptationSet();
        a.setLang(lang);
        return bitmovinApi.manifest.dash.addAudioAdaptationSetToPeriod(manifest, period, a);
    }

    private VideoAdaptationSet addVideoAdaptationSetToPeriod(DashManifest manifest, Period period) throws URISyntaxException, BitmovinApiException, RestException, UnirestException, IOException
    {
        VideoAdaptationSet adaptationSet = new VideoAdaptationSet();
        adaptationSet = bitmovinApi.manifest.dash.addVideoAdaptationSetToPeriod(manifest, period, adaptationSet);
        return adaptationSet;
    }

    private DashManifest createDashManifest(String name, EncodingOutput output) throws URISyntaxException, BitmovinApiException, UnirestException, IOException
    {
        DashManifest manifest = new DashManifest();
        manifest.setName(name);
        manifest.addOutput(output);
        manifest = bitmovinApi.manifest.dash.create(manifest);
        return manifest;
    }

    private Period addPeriodToDashManifest(DashManifest manifest) throws URISyntaxException, BitmovinApiException, RestException, UnirestException, IOException
    {
        Period period = new Period();
        period = bitmovinApi.manifest.dash.createPeriod(manifest, period);
        return period;
    }
}
