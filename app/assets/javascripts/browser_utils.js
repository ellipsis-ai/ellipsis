define(function(require) {
  var URI = require('urijs');

  return {
    ensureYPosInView: function(yPos, footerHeight) {
      var visibleBottom = window.innerHeight + window.scrollY - footerHeight;

      if (yPos > visibleBottom) {
        window.scrollBy(0, yPos - visibleBottom);
      }
    },

    removeQueryParam: function(qpName) {
      var url = new URI();
      url.removeQuery(qpName);
      this.setURL(url.href());
    },

    replaceQueryParam: function(qpName, value) {
      var url = new URI();
      url.removeQuery(qpName).addQuery(qpName, value);
      this.setURL(url.href());
    },

    setURL: function(url) {
      window.history.replaceState({}, "", url);
    }
  };
});
