const timeToSeconds = {
    "Y": 365*24*60*60,
    "YM": 30*24*60*60,
    "W": 7*24*60*60,
    "D": 24*60*60,
    "H": 60*60,
    "M": 60,
    "S": 1,
};

const isXsDurationValid = (str) => {
    return /^-?P((([0-9]+Y([0-9]+M)?([0-9]+W)?([0-9]+D)?|([0-9]+M)([0-9]+W)?([0-9]+D)?|([0-9]+W)([0-9]+D)?|([0-9]+D))(T(([0-9]+H)([0-9]+M)?([0-9]+(\.[0-9]{1,12})?S)?|([0-9]+M)([0-9]+(\.[0-9]{1,12})?S)?|([0-9]+(\.[0-9]{1,12})?S)))?)|(T(([0-9]+H)([0-9]+M)?([0-9]+(\.[0-9]{1,12})?S)?|([0-9]+M)([0-9]+(\.[0-9]{1,12})?S)?|([0-9]+(\.[0-9]{1,12})?S))))$/
    .test(
        str
    );
};

const parseDesignator = (designator, amount) => {
    const parsedAmount = parseFloat(amount);
    if (isNaN(parsedAmount)) {
        return 0;
    }
    return timeToSeconds[designator] * parsedAmount;
}

const parsePeriod = (period) => {
    if (typeof period !== 'string' || period.length === 0) {
        return 0;
    }
    const [, years, months, weeks, days] = /^(?:(\d+)Y)?(?:(\d+)M)?(?:(\d+)W)?(?:(\d+)D)?$/g.exec(period) || [];
    return parseDesignator("Y", years) + parseDesignator("YM", months) + parseDesignator("W", weeks) + parseDesignator("D", days);
};

const parseTime = (time) => {
    if (typeof time !== 'string' || time.length === 0) {
        return 0;
    }
    const [, hours, minutes, seconds] = /^(?:(\d+)H)?(?:(\d+)M)?(?:(\d+(?:\.\d+)?)S)?$/g.exec(time) || [];
    return parseDesignator("H", hours) + parseDesignator("M", minutes) + parseDesignator("S", seconds);
};

module.exports.parseDuration = (str) => {
    if (str[0] === '-' || !isXsDurationValid(str)) {
        return null;
    }
    const durationComponents = str.split('T');
    const period = durationComponents[0].substr(1);
    const time = durationComponents[1];

    let seconds = parsePeriod(period) + parseTime(time);
    return seconds;
};