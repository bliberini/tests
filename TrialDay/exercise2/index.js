const parser = require('./parser');

if (process.argv.slice(2).length === 0) {
    console.log("Provide xs:duration to parse");
    process.exit();
}

const xsDuration = process.argv.slice(2)[0];

console.log(parser.parseDuration(xsDuration));