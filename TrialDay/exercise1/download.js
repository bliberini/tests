const https = require('https');
const fs = require('fs');
const printer = require('./terminalPrinter');
const consoleParser = require('./consoleParser');

const parameters = consoleParser.parseArguments();

const fileUrl = parameters.fileUrl;
const filePath = parameters.filePath;
const concurrentDownloads = parameters.concurrentDownloads;

printer.setStatusLines(concurrentDownloads);

const getFileNameAndPath = (fileNumber) => {
    return filePath + 'file_' + fileNumber + '.bin';
};

const download = (i) => {
    https.get(fileUrl).on('response', (response) => {
        const fileNameAndPath = getFileNameAndPath(i);
        const file = fs.createWriteStream(fileNameAndPath);
        const fileSize = parseInt(response.headers['content-length']);

        let downloadedSize = 0;

        response.on('data', (data) => {
            file.write(data);
            downloadedSize += data.length;
            const downloadedPct = Math.round((downloadedSize / fileSize * 100) * 100) / 100;
            printer.printStatus(i, downloadedPct);
        })
        .on('end', () => {
            file.end();
            printer.printStatus(i, 100);
        })
        .on('error', (error) => {
            printer.printStatus(i, 0, 'There was an error downloading file: ' + error.message);
            fs.unlinkSync(fileNameAndPath);
        });
    });
}

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
