define(function(require) {
  var moment = require('moment');
  var ONE_WEEK_IN_MS = 1000 * 60 * 60 * 24 * 7;

  return {
    formatTimestampLong: function(timestamp) {
      const m = moment(timestamp);
      return `${m.format('dddd, LL')} at ${m.format('LT')}`;
    },

    formatTimestampRelative: function(timestamp) {
      return moment(timestamp).fromNow();
    },

    formatTimestampRelativeIfRecent: function(timestamp) {
      var then = timestamp;
      var now = Date.now();
      var diff = now - then > 0 ? now - then : then - now;
      if (diff < ONE_WEEK_IN_MS) {
        return this.formatTimestampRelative(timestamp);
      } else {
        return this.formatTimestampShort(timestamp);
      }
    },

    formatTimestampShort: function(timestamp) {
      return moment(timestamp).format('ll, LT');
    },

    formatCamelCaseIdentifier: function(phrase) {
      var split = phrase.split(' ').map((ea) => ea.replace(/[^\w$]/ig, ''));
      var firstWord = split[0].charAt(0).toLowerCase() + split[0].slice(1);
      var camel = firstWord + split.slice(1).map((ea) => ea.charAt(0).toUpperCase() + ea.slice(1)).join("");
      if (/^[^a-z_$]/i.test(phrase.charAt(0))) {
        camel = "_" + camel;
      }
      return camel;
    },

    formatEnvironmentVariableName: function(name) {
      return name.toUpperCase().replace(/\s/g, '_').replace(/^\d|[^A-Z0-9_]/g, '');
    },

    formatList: function(list, optionalMapper) {
      var mapper = optionalMapper || ((ea) => ea);
      if (list.length === 0) {
        return "";
      } else if (list.length === 1) {
        return list.map(mapper).join("");
      } else if (list.length === 2) {
        return list.map(mapper).join(" and ");
      } else if (list.length > 2) {
        return list.slice(0, list.length - 1).map(mapper).join(", ") +
            ", and " + list.slice(list.length - 1).map(mapper).join("");
      }
    }
  };
});
