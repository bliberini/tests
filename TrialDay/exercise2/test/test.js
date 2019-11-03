const expect = require('chai').expect;
const parser = require('../parser');

const invalidInputs = [
    'random',       // random letters
    '1',            // single number
    '1,2',          // number with comma
    'T1M',          // string not starting with P
    'P1',           // period without designator
    'P1H',          // period with time designator
    'P1YT1D',       // time with period designator
    'P1Y1',         // amount without following designator
    'P-1,23Y',      // period designator with negative amount
    'PT-1,23H',     // time designator with negative amount
    'PT',           // period and time without designators
    'P0.5YT',       // only seconds can be decimal
    'P0.5MT',       // only seconds can be decimal
    'P0.5WT',       // only seconds can be decimal
    'P0.5DT',       // only seconds can be decimal
    'PT0.5H',       // only seconds can be decimal
    'PT0.5M',       // only seconds can be decimal
    'PT0.S',        // if there's a decimal point, 1 to 12 digits must follow
    'PT0.1234567891234S', // if there's a decimal point, 1 to 12 digits must follow
];

const validInputs = [
    { input: 'P1Y1M1W1DT1H1M1S', value: 34822861 },     // full use of designators
    { input: 'P20M', value: 51840000 },                 // only period designator
    { input: 'PT20M', value: 1200 },                    // only time designator
    { input: 'PT200000S', value: 200000 },              // only seconds should be the same as seconds amount
    { input: 'P0Y', value: 0 },                         // period designator with 0 amount should return 0
    { input: 'PT0H', value: 0 },                        // time designator with 0 amount should return 0
    { input: 'PT0.5S', value: 0.5 },                    // time designator with comma-separated decimal amount
];

it('Empty should return null', (done) => {
    expect(parser.parseDuration('')).to.be.null;
    done();
});

it('Invalid strings should return null', (done) => {
    invalidInputs.forEach((input) => {
        expect(parser.parseDuration(input)).to.be.null;
    });
    done();
});

it('Valid strings should return corect value', (done) => {
    validInputs.forEach((inputObject) => {
        expect(parser.parseDuration(inputObject.input)).to.be.equal(inputObject.value);
    });
    done();
});