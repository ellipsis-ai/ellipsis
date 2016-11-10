define(function(require) {
  var moment = require('moment');
  var ONE_WEEK_IN_MS = 1000 * 60 * 60 * 24 * 7;

  return {
    formatTimestampLong: function(timestamp) {
      return moment(timestamp).format('ll, LTS');
    },

    formatTimestampRelative: function(timestamp) {
      return moment(timestamp).fromNow();
    },

    formatTimestampRelativeIfRecent: function(timestamp) {
      var then = timestamp;
      var now = Date.now();
      if (now - then < ONE_WEEK_IN_MS) {
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
    }
  };
});
