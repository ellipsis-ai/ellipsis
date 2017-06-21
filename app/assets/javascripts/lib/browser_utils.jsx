define(function(require) {
  var URI = require('urijs');

  return {
    ensureYPosInView: function(yPos, footerHeight) {
      var visibleBottom = window.innerHeight + window.scrollY - footerHeight;

      if (yPos > visibleBottom) {
        window.scrollBy(0, yPos - visibleBottom);
      }
    },

    hasQueryParam: function(qpName) {
      var url = new URI();
      return url.hasQuery(qpName);
    },

    removeQueryParam: function(qpName) {
      var url = new URI();
      url.removeQuery(qpName);
      this.replaceURL(url.href());
    },

    replaceQueryParam: function(qpName, value) {
      var url = new URI();
      url.removeQuery(qpName).addQuery(qpName, value);
      this.replaceURL(url.href());
    },

    replaceURL: function(url) {
      window.history.replaceState({}, "", url);
    },

    loadURL: function(url) {
      window.location.href = url;
    }
  };
});
