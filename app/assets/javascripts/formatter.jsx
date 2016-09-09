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
    }
  };
});
