"use strict";

const moment = require('moment-timezone');

const EventFormatter = {
  formats: {
    ALL_DAY: 'll',
    DATE: 'ddd, MMM D',
    YMD: 'Y-MM-DD',
    TIME: 'LT',
    TZ: 'z'
  },

  verbiage: {
    DASH: '—',
    ALL_DAY_SUFFIX: '(all day)',
    TODAY: 'Today'
  },

  defaultTimeZone: 'UTC',

  formatHangoutLinkFor(event, options) {
    const separator = options && options.details ? "" : " · ";
    return event.hangoutLink ? `${separator}[Join hangout](${event.hangoutLink})` : "";
  },

  formatAttendeesFor(event) {
    const attendees = event.attendees || [];
    const attendeesWithoutSelf = attendees.filter(ea => !ea.self);
    return attendeesWithoutSelf.length ? (" - " + attendeesWithoutSelf.map(ea => ea.displayName || ea.email).join(", ")) : "";
  },

  formatEvent: function(event, tz, optionalTodayYMD, options) {
    if (options && options.details) {
      return this.formatEventWithDetails(event, tz, optionalTodayYMD, options);
    } else {
      return this.formatEventWithoutDetails(event, tz, optionalTodayYMD, options);
    }
  },

  summaryLink: function(event) {
    const linkText = `${event.summary}${this.formatAttendeesFor(event)}`;
    const escaped = linkText.replace(/([\[\]<>])/g, "\\$1");
    return `[${escaped}](${event.htmlLink})`;
  },

  formatEventWithDetails: function(event, tz, optionalTodayYMD, options) {
    const time = this.formatEventDateTime(event, tz, optionalTodayYMD);
    let optionalData = "";
    if (event.description) {
      optionalData += `${event.description}  \n`;
    }
    if (event.location) {
      optionalData += `_Where: ${event.location}_  \n`;
    }
    return `${time}  \n**${this.summaryLink(event)}**  \n${optionalData}${this.formatHangoutLinkFor(event, options)}`;
  },

  formatEventWithoutDetails: function(event, tz, optionalTodayYMD, options) {
    const time = this.formatEventDateTime(event, tz, optionalTodayYMD);
    return `${time}: **${this.summaryLink(event)}**${this.formatHangoutLinkFor(event, options)}`;
  },

  formatEventDateTime: function(event, tz, optionalTodayYMD) {
    if (!event) {
      return "";
    } else if (event.start.date) {
      return this.formatAllDayEvent(event, optionalTodayYMD);
    } else {
      return this.formatRegularEvent(event, tz, optionalTodayYMD);
    }
  },

  formatAllDayEvent: function(event, optionalTodayYMD) {
    const formattedStartDate = moment(event.start.date).format(this.formats.ALL_DAY);
    const sameAsToday = optionalTodayYMD && moment(event.start.date).format(this.formats.YMD) === optionalTodayYMD;
    let formattedEventTime = sameAsToday ? this.verbiage.TODAY : formattedStartDate;
    if (!event.endTimeUnspecified && event.end.date) {
      let formattedEndDate = moment(event.end.date).subtract(1, 'days').format(this.formats.ALL_DAY);
      if (formattedEndDate !== formattedStartDate) {
        formattedEventTime += ` ${this.verbiage.DASH} ${formattedEndDate}`;
      }
    }
    if (formattedEventTime === formattedStartDate || formattedEventTime === this.verbiage.TODAY) {
      formattedEventTime += ` ${this.verbiage.ALL_DAY_SUFFIX}`;
    }
    return formattedEventTime;
  },

  formatRegularEvent: function(event, tz, optionalTodayYMD) {
    const eventTz = event.start.timeZone || tz || this.defaultTimeZone;
    let start = moment(event.start.dateTime).tz(eventTz);
    let startDate = start.format(this.formats.DATE);
    let startTime = start.format(this.formats.TIME);
    let end;
    let endDate = '';
    let endTime = '';
    if (!event.endTimeUnspecified) {
      end = moment(event.end.dateTime).tz(eventTz);
      endDate = end.format(this.formats.DATE);
      endTime = end.format(this.formats.TIME);
    }

    let excludeDate = false;
    if (optionalTodayYMD) {
      const sameStartDate = start.format(this.formats.YMD) === optionalTodayYMD;
      excludeDate = sameStartDate && (!endDate || endDate === startDate);
    }

    let formattedEventTime = excludeDate ? startTime : `${startDate} ${startTime}`;
    if (endDate && endDate !== startDate) {
      formattedEventTime += ` ${this.verbiage.DASH} ${endDate} ${endTime}`;
    } else if (endTime) {
      formattedEventTime += ` ${this.verbiage.DASH} ${endTime}`;
    }

    formattedEventTime += ` ${start.format(this.formats.TZ)}`;
    return formattedEventTime;
  }
};

module.exports = EventFormatter;
