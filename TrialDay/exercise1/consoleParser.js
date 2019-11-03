const printer = require('./terminalPrinter');
const url = require('url');

const parseArguments = () => {
    if (process.argv.indexOf('-url') < 0 || process.argv.indexOf('-file') < 0 ) {
        printer.printError('Usage error: node download.js -url <download URL> -file <file/path/> [-downloads <concurrent downloads>]');
        process.exit();
    }
    
    const fileUrl = url.parse(process.argv[process.argv.indexOf('-url') + 1]);
    const filePath = process.argv[process.argv.indexOf('-file') + 1];
    
    const concurrentDownloads = process.argv.indexOf('-downloads') > -1 ?
        parseInt(process.argv[process.argv.indexOf('-downloads') + 1])
        :
        1;
    if (concurrentDownloads > 10 || concurrentDownloads < 1) {
        printer.printError("Concurrent downloads must be greater than 0 and less or equal to 10");
        process.exit();
    }

    return {
        fileUrl,
        filePath,
        concurrentDownloads,
    };
};

const parseArgumentsS3 = () => {
    if (process.argv.indexOf('-bucketname') < 0 || process.argv.indexOf('-filekey') < 0 || process.argv.indexOf('-filepath') < 0 ) {
        console.error('Usage error: node s3download.js -bucketname <bucket name> -filekey <bucket file key> -filepath <file/path/> [-downloads <concurrent downloads>]');
        process.exit();
    }
    
    const bucket = process.argv[process.argv.indexOf('-bucketname') + 1];
    const filePath = process.argv[process.argv.indexOf('-filepath') + 1];
    const fileKey = process.argv[process.argv.indexOf('-filekey') + 1];
    
    const concurrentDownloads = process.argv.indexOf('-downloads') > -1 ?
        parseInt(process.argv[process.argv.indexOf('-downloads') + 1])
        :
        1;
    if (concurrentDownloads > 10 || concurrentDownloads < 1) {
        console.error("Concurrent downloads must be greater than 0 and less than 10");
        process.exit();
    }

    return {
        bucket,
        filePath,
        fileKey,
        concurrentDownloads,
    };
};

module.exports = {
    parseArguments,
    parseArgumentsS3,
};