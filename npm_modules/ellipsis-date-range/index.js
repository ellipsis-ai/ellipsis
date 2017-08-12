'use strict';

const moment = require('moment');
const chrono = require('chrono-node');

// NOTE:
// Chrono-node has a nice architecture and functionality but the ParsedResult.date
// function is a mystery. So I basically do the ParsedResult to Date conversion
// myself using the fantastic moment package.


function customChrono() {

  // Customize the defaul Chrono Parser so that we can
  // handle "year to date", "week to date", "month to date",
  // "last week", "last month", "last year".
  var ymwtoDateParser = new chrono.Parser();
  ymwtoDateParser.pattern = () => {
    return /year to date|ytd|month to date|mtd|week to date|wtd|this week|this month|this year/i
  }
  ymwtoDateParser.extract = (text, ref, match, opt) => {
    var startDate = null;
    var endDate = moment().utc();
    var range = 'year';
    if (match[0].match(/week|wtd/i)) {
      range = 'week'
    } else if (match[0].match(/month|mtd/i)) {
      range = 'month';
    }
    startDate = moment().utc().startOf(range);
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
  }

  // We create a Refiner to deal with:
  // 1. "last year", "last week", "last month"
  // 2. "April 2017", "May 2017", "Last April" and so on
  var lastYMWRefiner = new chrono.Refiner();
  lastYMWRefiner.refine = (text, results, opt) => {
    results.forEach((result) => {
      console.log(result);
      if (result.end === undefined && !result.tags.ENSlashDateFormatParser) {
        console.log("REF>>>>>>>>> ");
        result.end = result.start.clone();
        var startDate = moment.utc();
        startDate.set('year', result.start.get('year'));
        startDate.set('month', result.start.get('month')-1);
        startDate.set('date', result.start.get('day'));
        startDate.set('hour', result.start.get('hour'));
        startDate.set('minute', result.start.get('minute'));
        startDate.set('second', result.start.get('second'));
        startDate.set('millisecond', result.start.get('millisecond'));

        var endDate = startDate.clone();
        var range = 'day';
        if (result.text.match(/week/i)) {
          range = 'week'
        } else if (result.text.match(/month/i)) {
          range = 'month'
        } else if (result.text.match(/year/i)) {
          range = 'year'
        } else if (result.tags.ENMonthNameParser) {
          // This matches "last april" or "previous may"
          range = 'month'
        }
        startDate.utc().startOf(range);
        endDate.utc().endOf(range);
        result.tags.lastYMWRefiner=true;

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
    });
    return results;
  }

  var custom = new chrono.Chrono();
  custom.parsers.push(ymwtoDateParser);
  custom.refiners.push(lastYMWRefiner);
  return custom;
}

const DateRange = {
  getRange: (text) => {
    const r = customChrono().parse(text);
    if (r.length == 0 ) return null;
    var sDate = Date.UTC(
      r[0].start.get('year'),
      r[0].start.get('month') - 1,
      r[0].start.get('day'),
      r[0].start.get('hour'),
      r[0].start.get('minute'),
      r[0].start.get('second')
    );
    var eDate = Date.UTC(
      r[0].end.get('year'),
      r[0].end.get('month') - 1,
      r[0].end.get('day'),
      r[0].end.get('hour'),
      r[0].end.get('minute'),
      r[0].end.get('second')
    );
    return {
      start: new Date(sDate),
      end: new Date(eDate)
    }
  }
}

module.exports = DateRange;
