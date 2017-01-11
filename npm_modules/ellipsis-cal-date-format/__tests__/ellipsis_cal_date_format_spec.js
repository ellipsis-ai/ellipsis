"use strict";

jest.unmock('../index');
jest.unmock('moment-timezone');

const moment = require('moment-timezone');
const Formatter = require('../index');

function format(event, format_type) {
  return moment(event).format(Formatter.formats[format_type]);
}

describe("Formatter", () => {
  // The end date for all-day events is always one more than the last day of the event
  describe("formatAllDayEvent", () => {
    it("subtracts the last day from all day events", () => {
      const event = {
        start: {
          date: '2017-01-01'
        },
        end: {
          date: '2017-01-03'
        }
      };
      expect(Formatter.formatAllDayEvent(event)).toBe([
        format(event.start.date, 'ALL_DAY'),
        Formatter.verbiage.DASH, format('2017-01-02', 'ALL_DAY')
      ].join(" "));
    });

    it("includes “all day” for single day events", () => {
      const event1 = {
        start: {
          date: '2017-01-01'
        },
        end: {
          date: '2017-01-02'
        }
      };
      expect(Formatter.formatAllDayEvent(event1)).toBe([
        format(event1.start.date, 'ALL_DAY'),
        Formatter.verbiage.ALL_DAY_SUFFIX
      ].join(" "));
    });
  });

  describe("formatRegularEvent", () => {
    describe("it includes the end if specified", () => {
      it("includes the end time, and end date if different", () => {
        const event = {
          start: {
            dateTime: '2017-01-01T12:00:00.00Z'
          },
          end: {
            dateTime: '2017-01-02T17:00:00.00Z'
          }
        };
        expect(Formatter.formatRegularEvent(event)).toBe([
          format(event.start.dateTime, 'DATE'),
          format(event.start.dateTime, 'TIME'),
          Formatter.verbiage.DASH,
          format(event.end.dateTime, 'DATE'),
          format(event.end.dateTime, 'TIME')
        ].join(" "));
      });

      it("omits the end date if the same", () => {
        const event = {
          start: {
            dateTime: '2017-01-01T12:00:00.00Z'
          },
          end: {
            dateTime: '2017-01-01T17:00:00.00Z'
          }
        };
        expect(Formatter.formatRegularEvent(event)).toBe([
          format(event.start.dateTime, 'DATE'),
          format(event.start.dateTime, 'TIME'),
          Formatter.verbiage.DASH,
          format(event.end.dateTime, 'TIME')
        ].join(" "));
      });
    });

    it("omits the end if unspecified", () => {
      const event = {
        start: {
          dateTime: '2017-01-01T12:00:00.00Z'
        },
        endTimeUnspecified: true
      };
      expect(Formatter.formatRegularEvent(event)).toBe([
        format(event.start.dateTime, 'DATE'),
        format(event.start.dateTime, 'TIME')
      ].join(" "));
    });
  });
});
