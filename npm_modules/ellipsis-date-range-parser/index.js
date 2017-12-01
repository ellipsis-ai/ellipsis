'use strict';

const moment = require('moment-timezone');
const chrono = require('chrono-node');

// This module is a simple layer on top of chrono-node. The module exports one
// function that takes text and returns Date range it finds in the text. If no
// date range is found the function returns null.
//
// NOTE 1:
// Chrono-node has a nice architecture and functionality but the ParsedResult.date
// function is a mystery. So I basically do the ParsedResult to Date conversion
// myself using the fantastic moment package.
//
// NOTE 2:
// Whatever date is parsed is assumed to be in UTC. For example: if you are in
// PDT and type "last week" we first convert now to UTC than go back by one
// week.

const defaultTimeZone = 'UTC';

// This is an helper method. It should always be private.
const setMomentObject = (momentDate, parsedComponent) => {
    return momentDate
      .set('year', parsedComponent.get('year'))
      .set('month', parsedComponent.get('month') - 1)
      .set('date', parsedComponent.get('day'))
      .set('hour', parsedComponent.get('hour'))
      .set('minute', parsedComponent.get('minute'))
      .set('second', parsedComponent.get('second'))
      .set('millisecond', parsedComponent.get('millisecond'));
};

function customChrono(timeZone) {

  var userTimezone = timeZone ||  defaultTimeZone;
  // Handle:
  //   "year to date", "week to date", "month to date",
  //   "this week", "this month", "this year".
  var ymwtoDateParser = new chrono.Parser();
  ymwtoDateParser.pattern = () => {
    return /year to date|ytd|month to date|mtd|week to date|wtd|this week|this month|this year/i;
  };

  // eslint-disable-next-line no-unused-vars
  ymwtoDateParser.extract = (text, ref, match, opt) => {
    var range = 'year';
    if (match[0].match(/week|wtd/i)) {
      range = 'week';
    } else if (match[0].match(/month|mtd/i)) {
      range = 'month';
    }
    var startDate = moment().tz(userTimezone).startOf(range);
    var endDate = moment().tz(userTimezone);
    var parsedResult = new chrono.ParsedResult({
      ref: ref,
      text: match[0],
      index: match.index,
      tags: { ymwtoDateParser: true },
      start: {
        day: startDate.date(),
        month: startDate.month() + 1,
        hour: startDate.hour(),
        minute: startDate.minute(),
        second: startDate.seconds()
      },
      end: {
        day: endDate.date(),
        month: endDate.month() + 1,
        hour: endDate.hour(),
        minute: endDate.minute(),
        second: endDate.seconds()
      }
    });
    return parsedResult;
  };

  // Handle:
  //   "last year", "last week", "last month"
  //   "April 2017", "May 2017", "Last April"
  //   "1/12/2026"
  var lastYMWRefiner = new chrono.Refiner();
  // eslint-disable-next-line no-unused-vars
  lastYMWRefiner.refine = (text, results, opt) => {
    results.forEach((result) => {
      if (result.end === undefined) {
        // Handle text with slash formatted dates like
        // 04/01/2017.
        if (result.tags.ENSlashDateFormatParser) {
          result.end = result.start.clone();
        } else {
          result.end = result.start.clone();

          // Chrono-node has a method that takes a ParseResult and returns a Moment date
          // but it does some weird math and a timezoneOffset. So I do the conversion myself.
          var startDate = setMomentObject(moment().tz(userTimezone),result.start);
          var endDate = startDate.clone();
          var range = 'day';
          if (result.text.match(/week/i)) {
            range = 'isoWeek';
            // Chrone makes the week start on Sunday. We like it to start on
            // Monday, just like 'isoWeek' in moment
            startDate = moment().tz(userTimezone).subtract(1, 'week').startOf(range).set('millisecond', 0);
          } else if (result.text.match(/month/i)) {
            range = 'month';
          } else if (result.text.match(/year/i)) {
            range = 'year';
          } else if (result.tags.ENMonthNameParser) {
            // This matches "last april" or "previous may"
            range = 'month';
          }
          startDate.startOf(range);
          endDate.endOf(range);

          result.start.imply('day', startDate.date());
          result.start.imply('month',startDate.month() + 1);
          result.start.imply('hour', 0);
          result.start.imply('minute',0);
          result.start.imply('second',0);

          result.end.imply('day', endDate.date());
          result.end.imply('month', endDate.month() + 1);
          result.end.imply('hour', endDate.hours());
          result.end.imply('minute', endDate.minutes());
          result.end.imply('second', endDate.seconds());
        }
      }
    });
    return results;
  };

  // Last refiner makes sure the times on start and end dates
  // are consitent, meaning start time is 00:00:00 and
  // end time is 23:59:59
  var setStartTimeAndEndTimeRefiner = new chrono.Refiner();
  // eslint-disable-next-line no-unused-vars
  setStartTimeAndEndTimeRefiner.refine = (text, results, opt) => {
    results.forEach((result) => {
      result.start.assign('hour', 0);
      result.start.assign('minute',0);
      result.start.assign('second',0);
      result.start.assign('millisecond',0);

      if (result.end) {
        result.end.assign('hour', 23);
        result.end.assign('minute', 59);
        result.end.assign('second', 59);
        result.end.assign('millisecond',0);
      }
    });
    return results;
  };
  var custom = new chrono.Chrono();
  custom.parsers.push(ymwtoDateParser);
  custom.refiners.push(lastYMWRefiner);
  custom.refiners.push(setStartTimeAndEndTimeRefiner);
  return custom;
}

const DateRangeParser = {

  defaultTimeZone: defaultTimeZone,

  parse: (text, timeZoneToParse) => {
    const timeZone = timeZoneToParse && moment.tz.names().includes(timeZoneToParse) ? timeZoneToParse : defaultTimeZone;

    const r = customChrono(timeZone).parse(text);
    if (r.length === 0 ) return null;
    var sDate = setMomentObject(moment.tz(timeZone), r[0].start);
    var eDate = setMomentObject(moment.tz(timeZone), r[0].end);
    return {
      start: sDate.toDate(),
      end: eDate.toDate(),
      tz: timeZone
    };
  }
};

module.exports = DateRangeParser;
