define(function() {
  return {
    formatTimestampLong: function(timestamp) {
      var d = new Date(timestamp);
      // N.B. Safari doesn't support toLocaleString options at present
      return d.toLocaleString(void(0), {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: 'numeric',
        minute: 'numeric',
        second: 'numeric',
        timeZoneName: 'short'
      });
    },

    formatTimestampShort: function(timestamp) {
      var d = new Date(timestamp);
      return d.toLocaleString(void(0), {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: 'numeric',
        minute: 'numeric'
      });
    },
  };
});
