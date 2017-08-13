'use strict';
const DateRange = require('../index');
const moment = require('moment');

// This is to mock new Date() 2010-04-17T07:00:00.000Z
// const DATE_TO_USE = new Date('04/17/2010');
// const _Date = Date;
// global.Date = jest.fn(() => DATE_TO_USE);
// global.Date.UTC = _Date.UTC;
// global.Date.parse = _Date.parse;
// global.Date.now = _Date.now;


[
  { text: "last year", range: "year", minus: "year"  },
  { text: "last month", range: "month", minus: "month" },
  { text: "last week", range: "isoWeek", minus: "week" }
].forEach((i) => {
    test(i.text, () => {
      const r = DateRange.getRange(i.text);
      var eStartDate = moment.utc().subtract(1, i.minus).startOf(i.range).set('millisecond', 0);
      var eEndDate = moment.utc().subtract(1, i.minus).endOf(i.range).set('millisecond', 0);
      expect(r.start.toISOString()).toBe(eStartDate.toDate().toISOString());
      expect(r.end.toISOString()).toBe(eEndDate.toDate().toISOString());
    });
});

[
  { text: "year to date", range: "year" },
  { text: "ytd", range: "year" },
  { text: "YTD", range: "year" },
  { text: "this year", range: "year" },
  { text: "month to date", range: "month" },
  { text: "mtd", range: "month" },
  { text: "MTD", range: "month" },
  { text: "this month", range: "month" },
  { text: "week to date", range: "week" },
  { text: "wtd", range: "week" },
  { text: "WTD", range: "week" },
  { text: "this week", range: "week" }
].forEach((i) => {
    test(i.text, () => {
      const r = DateRange.getRange(i.text);
      var eStartDate = moment.utc().startOf(i.range).set('millisecond', 0);
      var eEndDate = moment.utc().set('hours', 23).set('minutes', 59).set('seconds', 59).set('millisecond', 0);
      expect(r.start.toISOString()).toBe(eStartDate.toDate().toISOString());
      expect(r.end.toISOString()).toBe(eEndDate.toDate().toISOString());
    });
});

[
  { text: "April 2016", eStart: moment.utc("2016-04-01T00:00:00.000Z"), eEnd: moment.utc("2016-04-30T23:59:59.000Z") },
  { text: "Apr 2009", eStart: moment.utc("2009-04-01T00:00:00.000Z"), eEnd: moment.utc("2009-04-30T23:59:59.000Z") },
  { text: "4/1/2016", eStart: moment.utc("2016-04-01T00:00:00.000Z"), eEnd: moment.utc("2016-04-01T23:59:59.000Z") },
  { text: "4/1/2017 - 5/1/2017", eStart: moment.utc("2017-04-01T00:00:00.000Z"), eEnd: moment.utc("2017-05-01T23:59:59.000Z") },
  { text: "1/1/2017 - 2/1/2017", eStart: moment.utc("2017-01-01T00:00:00.000Z"), eEnd: moment.utc("2017-02-01T23:59:59.000Z") },
  { text: "1/11/2017 - 2/11/2017", eStart: moment.utc("2017-01-11T00:00:00.000Z"), eEnd: moment.utc("2017-02-11T23:59:59.000Z") },
  { text: "1 April 2016 - 1 May 2017", eStart: moment.utc("2016-04-01T00:00:00.000Z"), eEnd: moment.utc("2017-05-01T23:59:59.000Z") },
  { text: "April 1 2016 - May 2 2017", eStart: moment.utc("2016-04-01T00:00:00.000Z"), eEnd: moment.utc("2017-05-02T23:59:59.000Z") }
].forEach((i) => {
    test(i.text, () => {
      const r = DateRange.getRange(i.text);
      expect(r.start.toISOString()).toBe(i.eStart.toDate().toISOString());
      expect(r.end.toISOString()).toBe(i.eEnd.toDate().toISOString());
    });
});

[
  { text: "I will arrive home on April 2016", eStart: moment.utc("2016-04-01T00:00:00.000Z"), eEnd: moment.utc("2016-04-30T23:59:59.000Z") },
  { text: "I learn Italian in Apr 2009", eStart: moment.utc("2009-04-01T00:00:00.000Z"), eEnd: moment.utc("2009-04-30T23:59:59.000Z") },
  { text: "you have to finish all by 4/1/2016", eStart: moment.utc("2016-04-01T00:00:00.000Z"), eEnd: moment.utc("2016-04-01T23:59:59.000Z") },
  { text: "The Olympic games will be 4/1/2016 - 5/1/2017", eStart: moment.utc("2016-04-01T00:00:00.000Z"), eEnd: moment.utc("2017-05-01T23:59:59.000Z") },
  { text: "The Olympic games will from 4/13/2016 to 5/16/2017", eStart: moment.utc("2016-04-13T00:00:00.000Z"), eEnd: moment.utc("2017-05-16T23:59:59.000Z") },
  { text: "from 1 April 2016 to 1 May 2017", eStart: moment.utc("2016-04-01T00:00:00.000Z"), eEnd: moment.utc("2017-05-01T23:59:59.000Z") },
  { text: "from April 1 2016 to  May 2 2017", eStart: moment.utc("2016-04-01T00:00:00.000Z"), eEnd: moment.utc("2017-05-02T23:59:59.000Z") }
].forEach((i) => {
    test(i.text, () => {
      const r = DateRange.getRange(i.text);
      expect(r.start.toISOString()).toBe(i.eStart.toDate().toISOString());
      expect(r.end.toISOString()).toBe(i.eEnd.toDate().toISOString());
    });
});
