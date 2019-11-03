const readline = require('readline');

let status = new Array();

const clearScreen = () => {
    const blank = '\n'.repeat(process.stdout.rows)
    console.log(blank)
    readline.cursorTo(process.stdout, 0, 0)
    readline.clearScreenDown(process.stdout)
};

const setStatusLines = (lines) => {
    status = new Array(lines);
}

const printStatus = (i, pct, message) => {
    const statusMessage = ('[file_' + i + '] ') + (
        pct === 100 ? 'Finished downloading' : ('Downloading ' + pct + '%')
    );
    const fileStatus = message ?
        ('[file_' + i + '] ' + message)
        :
        statusMessage;
    status[i - 1] = fileStatus
    clearScreen();
    console.log(status.join('\n'));
};

const printLog = (message) => {
    console.log(message);
};

const printError = (error) => {
    console.error(error);
};

module.exports = {
    setStatusLines,
    printError,
    printLog,
    printStatus,
};