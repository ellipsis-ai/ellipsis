"use strict";

const moment = require('moment-timezone');

const EventFormatter = {
  formats: {
    ALL_DAY: 'll',
    DATE: 'ddd, MMM D',
    TIME: 'LT',
    TZ: 'z'
  },

  verbiage: {
    DASH: '—',
    ALL_DAY_SUFFIX: '(all day)'
  },

  formatEvent: function(event, tz) {
    if (event.start.date) {
      return this.formatAllDayEvent(event);
    } else {
      return this.formatRegularEvent(event, tz);
    }
  },

  formatAllDayEvent: function(event) {
    const formattedStartDate = moment(event.start.date).format(this.formats.ALL_DAY);
    let formattedEventTime = formattedStartDate;
    if (!event.endTimeUnspecified && event.end.date) {
      let formattedEndDate = moment(event.end.date).subtract(1, 'days').format(this.formats.ALL_DAY);
      if (formattedEndDate !== formattedStartDate) {
        formattedEventTime += ` ${this.verbiage.DASH} ${formattedEndDate}`;
      }
    }
    if (formattedEventTime === formattedStartDate) {
      formattedEventTime += ` ${this.verbiage.ALL_DAY_SUFFIX}`;
    }
    return formattedEventTime;
  },

  formatRegularEvent: function(event, tz) {
    let start;
    if (event.start.timeZone || tz) {
      start = moment(event.start.dateTime).tz(event.start.timeZone || tz);
    } else {
      start = moment(event.start.dateTime);
    }
    let startDate = start.format(this.formats.DATE);
    let startTime = start.format(this.formats.TIME);
    let formattedEventTime = `${startDate} ${startTime}`;
    let end;
    let endDate = '';
    let endTime = '';
    if (!event.endTimeUnspecified) {
      if (event.start.timeZone || tz) {
        end = moment(event.end.dateTime).tz(event.start.timeZone || tz);
      } else {
        end = moment(event.end.dateTime);
      }
      endDate = end.format(this.formats.DATE);
      endTime = end.format(this.formats.TIME);
      if (endDate !== startDate) {
        formattedEventTime += ` ${this.verbiage.DASH} ${endDate} ${endTime}`;
      } else {
        formattedEventTime += ` ${this.verbiage.DASH} ${endTime}`;
      }
    }
    if (event.start.timeZone || tz) {
      formattedEventTime += ` ${start.format(this.formats.TZ)}`;
    }
    return formattedEventTime;
  }
};

module.exports = EventFormatter;
