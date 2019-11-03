const url = require('url');
const https = require('https');
const fs = require('fs');
const readline = require('readline');

if (process.argv.indexOf('-url') < 0 || process.argv.indexOf('-file') < 0 ) {
    console.error('Usage error: node download.js -url <download URL> -file <file/path/> [-downloads <concurrent downloads>]');
    return;
}

const fileUrl = url.parse(process.argv[process.argv.indexOf('-url') + 1]);
const filePath = process.argv[process.argv.indexOf('-file') + 1];

const concurrentDownloads = process.argv.indexOf('-downloads') > -1 ?
    parseInt(process.argv[process.argv.indexOf('-downloads') + 1])
    :
    1;
if (concurrentDownloads > 10 || concurrentDownloads < 1) {
    console.error("Concurrent downloads must be greater than 0 and less than 10");
    return;
}

const status = new Array(concurrentDownloads);

const clearScreen = () => {
    const blank = '\n'.repeat(process.stdout.rows)
    console.log(blank)
    readline.cursorTo(process.stdout, 0, 0)
    readline.clearScreenDown(process.stdout)
}

const printStatus = (i, pct, message) => {
    const statusMessage = ('[file_' + i + '] ') + (
        pct === 100 ? 'Finished downloading' : ('[file_' + i + '] Downloading ' + pct + '%')
    );
    const fileStatus = message ?
        ('[file_' + i + '] ' + message)
        :
        statusMessage;
    status[i - 1] = fileStatus
    clearScreen();
    console.log(status.join('\n'));
}

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
            printStatus(i, downloadedPct);
        })
        .on('end', () => {
            file.end();
            printStatus(i, 100);
        })
        .on('error', (error) => {
            console.error('There was an error downloading file: ' + error.message);
            fs.unlinkSync(fileNameAndPath);
        });
    });
}

for (let i = 0; i < concurrentDownloads; i++) {
    download(i+1);
}

process.on('SIGINT', () => {
    console.log('\nProgram interrupted. Deleting file(s)...');
    for (let i = 0; i < concurrentDownloads; i++) {
        fs.unlinkSync(getFileNameAndPath(i+1));
    }
    process.exit();
});
