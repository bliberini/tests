const fs = require('fs');
const readline = require('readline');
const aws = require('aws-sdk');
const printer = require('./terminalPrinter');
const consoleParser = require('./consoleParser');

printer.printLog('Remember to fill the aws.config.json file with your credentials.');

const parameters = consoleParser.parseArgumentsS3();

const filePath = parameters.filePath;
const bucket = parameters.bucket;
const fileKey = parameters.fileKey;
const concurrentDownloads = parameters.concurrentDownloads;

printer.setStatusLines(concurrentDownloads);

const download = (i) => {
    const file = fs.createWriteStream(getFileNameAndPath(i));
    const params = {
        Bucket: bucket,
        Key: fileKey,
    };
    s3.headObject(params, (err, data) => {
        if (err) {
            print.printError(i, 0, err.message,);
            return;
        }
        const fileSize = data.ContentLength;
        let downloadedSize = 0;
        s3.getObject(params)
            .createReadStream()
            .on('data', (data) => {
                file.write(data);
                downloadedSize += data.length;
                const downloadedPct = Math.round((downloadedSize / fileSize * 100) * 100) / 100;
                printer.printStatus(i, downloadedPct);
            })
            .on('error', (err) => {
                printer.printStatus(i, 0, err.message);
            })
            .on('end', () => {
                file.end();
            });
    });
}

aws.config.loadFromPath('./aws.config.json');
let s3 = new aws.S3();

const getFileNameAndPath = (fileNumber) => {
    return filePath + 'file_' + fileNumber + '.bin';
};

for (let i = 0; i < concurrentDownloads; i++) {
    download(i+1);
}

process.on('SIGINT', () => {
    printer.printLog('\nProgram interrupted. Deleting file(s)...');
    for (let i = 0; i < concurrentDownloads; i++) {
        fs.unlinkSync(getFileNameAndPath(i+1));
    }
    process.exit();
});
