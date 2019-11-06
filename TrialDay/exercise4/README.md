# Instructions to execute
1. In a terminal, execute `mvn install` to install dependencies
2. Edit `config.json` in `src/main/java` to add:
	* `bitmovinKey`: your Bitmovin key
	* `s3AccessKey`: your S3 access key
	* `s3SecretKey`: your S3 secret key
	* `s3BucketName`: your S3 bucket name where the video and audio files will be saved
	* `s3Region`: the name of the region where your bucket is deployed. It must follow the form of Bitmovin's CloudRegion enum names (e.g. us-east-2 is AWS_US_EAST_2)
	* `sourceHostUrl`: the host of your video's URL (e.g. myhost.com/)
	* `sourcePathUrl`: the path to your video's URL in the host (e.g. videos/myvideo.mp4)
