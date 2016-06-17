define(function(require) {
  return {
    getPathname: function() {
      return window.location.pathname;
    },

    getQueryParams: function() {
      return window.location.search;
    },

    ensureYPosInView: function(yPos, footerHeight) {
      var visibleBottom = window.innerHeight + window.scrollY - footerHeight;

      if (yPos > visibleBottom) {
        window.scrollBy(0, yPos - visibleBottom);
      }
    },

    removeQueryParam: function(qpName) {
      var path = this.getPathname();
      var queryParams = this.getQueryParams();
      var match = new RegExp('^' + qpName + '(=\\S+)?$');
      var newQueryParams = queryParams
        .replace(/^\?/, '')
        .split('&')
        .filter(function(qp) { return !qp.match(match); })
        .join('&');
      var newURL = path + (newQueryParams ? '?' + newQueryParams : '');
      if (newURL !== path + queryParams) {
        this.setURL(newURL);
      }
    },

    setURL: function(url) {
      window.history.replaceState({}, "", url);
    }
  };
});
