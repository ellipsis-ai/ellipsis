const a = {
  "Today": "08/01/2017",
  "Week starts": "Monday",
  "last year": "01/01/2016 - 01/31/2016",
  "last month": "07/01/2017 - 07/31/2017",
  "last week": "07/24/2017 - 07/30/2017",
  "year to date": "07/24/2017 - 07/30/2017",
  "month to date": "07/24/2017 - 07/30/2017",
  "week to date": "07/24/2017 - 07/30/2017",
  "this year": "year to date",
  "this month": "month to date",
  "this week": "week to date"
}

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


// [
//   { text: "last year", range: "year" },
//   { text: "last month", range: "month" },,
//   { text: "last week", range: "week" },
//
// ].forEach((i) => {
//     test(i.text, () => {
//       const r = DateRange.getRange(i.text);
//       var eStartDate = moment.utc().subtract(1, i.range).startOf(i.range).set('millisecond', 0);
//       var eEndDate = moment.utc().subtract(1, i.range).endOf(i.range).set('millisecond', 0);
//       expect(r.start.toISOString()).toBe(eStartDate.toDate().toISOString());
//       expect(r.end.toISOString()).toBe(eEndDate.toDate().toISOString())
//     });
// });
//
// [
//   { text: "year to date", range: "year" },
//   { text: "ytd", range: "year" },
//   { text: "YTD", range: "year" },
//   { text: "this year", range: "year" },
//   { text: "month to date", range: "month" },
//   { text: "mtd", range: "month" },
//   { text: "MTD", range: "month" },
//   { text: "this month", range: "month" },
//   { text: "week to date", range: "week" },
//   { text: "wtd", range: "week" },
//   { text: "WTD", range: "week" },
//   { text: "this week", range: "week" },
//
// ].forEach((i) => {
//     test(i.text, () => {
//       const r = DateRange.getRange(i.text);
//       var eStartDate = moment.utc().startOf(i.range).set('millisecond', 0);
//       var eEndDate = moment.utc().set('millisecond', 0);
//       expect(r.start.toISOString()).toBe(eStartDate.toDate().toISOString());
//       expect(r.end.toISOString()).toBe(eEndDate.toDate().toISOString());
//     });
// });

[
  // { text: "April 2016", eStart: moment.utc("2016-04-01T00:00:00.000Z"), eEnd: moment.utc("2016-04-30T23:59:59.000Z") },
  // { text: "Apr 2009", eStart: moment.utc("2009-04-01T00:00:00.000Z"), eEnd: moment.utc("2009-04-30T23:59:59.000Z") },
  { text: "4/1/2016", eStart: moment.utc("2016-04-01T00:00:00.000Z"), eEnd: moment.utc("2016-04-01T23:59:59.000Z") },
  { text: "4/1/2016 - 5/1/2017", eStart: moment.utc("2016-04-01T12:00:00.000Z"), eEnd: moment.utc("2017-05-01T12:00:00.000Z") },
].forEach((i) => {
    test(i.text, () => {
      const r = DateRange.getRange(i.text);
      expect(r.start.toISOString()).toBe(i.eStart.toDate().toISOString());
      expect(r.end.toISOString()).toBe(i.eEnd.toDate().toISOString());
    });
});
