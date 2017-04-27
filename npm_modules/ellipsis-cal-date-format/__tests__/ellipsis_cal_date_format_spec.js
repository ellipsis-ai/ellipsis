"use strict";

jest.unmock('../index');
jest.unmock('moment-timezone');

const moment = require('moment-timezone');
const Formatter = require('../index');

function format(event, format_type) {
  return moment(event).format(Formatter.formats[format_type]);
}

function formatTz(event, format_type, tz) {
  return moment(event).tz(tz || 'UTC').format(Formatter.formats[format_type]);
}

describe("Formatter", () => {

  describe("formatEventWithoutDetails", () => {
    it("Displays non-time parts of the event without details", () => {
      const event = {
        summary: "Some made-up event",
        htmlLink: "https://calendar.google.com/calendar",
        hangoutLink: "https://hangout.google.com/",
        start: {
          date: '2017-01-01'
        },
        end: {
          date: '2017-01-03'
        }
      };
      expect(Formatter.formatEventWithoutDetails(event)).toBe([
        format(event.start.date, 'ALL_DAY'),
        Formatter.verbiage.DASH,
        format('2017-01-02', 'ALL_DAY') + ":",
        `**[${event.summary}](${event.htmlLink})** · [Join hangout](${event.hangoutLink})`
      ].join(" "));
    });

  });

  describe("formatEventWithDetails", () => {

    it("Displays non-time parts of the event with details", () => {
      const event = {
        summary: "Some made-up event",
        description: "Some made-up description",
        location: "Toronto",
        htmlLink: "https://calendar.google.com/calendar",
        hangoutLink: "https://hangout.google.com/",
        start: {
          date: '2017-01-01'
        },
        end: {
          date: '2017-01-03'
        }
      };
      expect(Formatter.formatEventWithDetails(event, null, null, { details: true })).toBe([
        format(event.start.date, 'ALL_DAY') + " ",
        Formatter.verbiage.DASH + " ",
        format('2017-01-02', 'ALL_DAY') + "  \n",
        `**[${event.summary}](${event.htmlLink})**  \n${event.description}  \n_Where: ${event.location}_  \n[Join hangout](${event.hangoutLink})`
      ].join(""));
    });
  });

  describe("formatEvent", () => {

    it("Calls the appropriate formatting method given details=true", () => {
      const MyFormatter = require('../index');
      MyFormatter.formatEventWithDetails = jest.fn();
      MyFormatter.formatEvent({}, null, null, { details: true });
      expect(MyFormatter.formatEventWithDetails.mock.calls.length).toBe(1);
    });

    it("Calls the appropriate formatting method given details=false", () => {
      const MyFormatter = require('../index');
      MyFormatter.formatEventWithoutDetails = jest.fn();
      MyFormatter.formatEvent({}, null, null, { details: false });
      expect(MyFormatter.formatEventWithoutDetails.mock.calls.length).toBe(1);
    });

  });

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


    it('says Today for all-day event that is the same day as specified date', () => {
      const event1 = {
        start: {
          date: '2017-01-01'
        },
        end: {
          date: '2017-01-02'
        }
      };
      expect(Formatter.formatAllDayEvent(event1, '2017-01-01')).toBe([
        Formatter.verbiage.TODAY,
        Formatter.verbiage.ALL_DAY_SUFFIX
      ].join(" "));
    });
  });

  describe("formatRegularEvent", () => {
    it("includes the end time and end date if different", () => {
      const event = {
        start: {
          dateTime: '2017-01-01T12:00:00.00Z'
        },
        end: {
          dateTime: '2017-01-02T17:00:00.00Z'
        }
      };
      expect(Formatter.formatRegularEvent(event)).toBe([
        formatTz(event.start.dateTime, 'DATE'),
        formatTz(event.start.dateTime, 'TIME'),
        Formatter.verbiage.DASH,
        formatTz(event.end.dateTime, 'DATE'),
        formatTz(event.end.dateTime, 'TIME'),
        formatTz(event.start.dateTime, 'TZ')
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
        formatTz(event.start.dateTime, 'DATE'),
        formatTz(event.start.dateTime, 'TIME'),
        Formatter.verbiage.DASH,
        formatTz(event.end.dateTime, 'TIME'),
        formatTz(event.start.dateTime, 'TZ')
      ].join(" "));
    });

    it("omits the end if unspecified", () => {
      const event = {
        start: {
          dateTime: '2017-01-01T12:00:00.00Z'
        },
        endTimeUnspecified: true
      };
      expect(Formatter.formatRegularEvent(event)).toBe([
        formatTz(event.start.dateTime, 'DATE'),
        formatTz(event.start.dateTime, 'TIME'),
        formatTz(event.start.dateTime, 'TZ')
      ].join(" "));
    });

    it("defaults to UTC with no time zone specified", () => {
      const event = {
        start: {
          dateTime: '2017-01-01T12:00:00.00-05:00'
        },
        end: {
          dateTime: '2017-01-02T17:00:00.00-05:00'
        }
      };
      expect(Formatter.formatRegularEvent(event)).toBe([
        formatTz(event.start.dateTime, 'DATE', 'UTC'),
        formatTz(event.start.dateTime, 'TIME', 'UTC'),
        Formatter.verbiage.DASH,
        formatTz(event.end.dateTime, 'DATE', 'UTC'),
        formatTz(event.end.dateTime, 'TIME', 'UTC'),
        formatTz(event.start.dateTime, 'TZ', 'UTC')
      ].join(" "));
    });

    it("uses the time zone of the event if specified", () => {
      const event = {
        start: {
          dateTime: '2017-01-01T12:00:00.00-05:00',
          timeZone: 'America/Toronto'
        },
        end: {
          dateTime: '2017-01-02T17:00:00.00-05:00',
          timeZone: 'America/Toronto'
        }
      };
      expect(Formatter.formatRegularEvent(event)).toBe([
        formatTz(event.start.dateTime, 'DATE', 'America/Toronto'),
        formatTz(event.start.dateTime, 'TIME', 'America/Toronto'),
        Formatter.verbiage.DASH,
        formatTz(event.end.dateTime, 'DATE', 'America/Toronto'),
        formatTz(event.end.dateTime, 'TIME', 'America/Toronto'),
        formatTz(event.start.dateTime, 'TZ', 'America/Toronto')
      ].join(" "));
    });

    it("uses a specified time zone if the event has none", () => {
      const event = {
        start: {
          dateTime: '2017-01-01T12:00:00.00Z'
        },
        end: {
          dateTime: '2017-01-02T17:00:00.00Z'
        }
      };
      expect(Formatter.formatRegularEvent(event, 'America/Toronto')).toBe([
        formatTz(event.start.dateTime, 'DATE', 'America/Toronto'),
        formatTz(event.start.dateTime, 'TIME', 'America/Toronto'),
        Formatter.verbiage.DASH,
        formatTz(event.end.dateTime, 'DATE', 'America/Toronto'),
        formatTz(event.end.dateTime, 'TIME', 'America/Toronto'),
        formatTz(event.start.dateTime, 'TZ', 'America/Toronto')
      ].join(" "));
    });

    it("prefers the event’s time zone", () => {
      const event = {
        start: {
          dateTime: '2017-01-01T12:00:00.00-05:00',
          timeZone: 'America/Toronto'
        },
        end: {
          dateTime: '2017-01-02T17:00:00.00-05:00',
          timeZone: 'America/Toronto'
        }
      };
      expect(Formatter.formatRegularEvent(event, 'America/Los_Angeles')).toBe([
        formatTz(event.start.dateTime, 'DATE', 'America/Toronto'),
        formatTz(event.start.dateTime, 'TIME', 'America/Toronto'),
        Formatter.verbiage.DASH,
        formatTz(event.end.dateTime, 'DATE', 'America/Toronto'),
        formatTz(event.end.dateTime, 'TIME', 'America/Toronto'),
        formatTz(event.start.dateTime, 'TZ', 'America/Toronto')
      ].join(" "));
    });

    it("excludes the date if start and end are same as specified date", () => {
      const event = {
        start: {
          dateTime: '2017-01-01T12:00:00.00-05:00',
          timeZone: 'America/Toronto'
        },
        end: {
          dateTime: '2017-01-01T17:00:00.00-05:00',
          timeZone: 'America/Toronto'
        }
      };
      expect(Formatter.formatRegularEvent(event, 'America/Toronto', '2017-01-01')).toBe([
        formatTz(event.start.dateTime, 'TIME', 'America/Toronto'),
        Formatter.verbiage.DASH,
        formatTz(event.end.dateTime, 'TIME', 'America/Toronto'),
        formatTz(event.start.dateTime, 'TZ', 'America/Toronto')
      ].join(" "));
    });
  });
});
