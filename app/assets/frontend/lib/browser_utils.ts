import * as URI from 'urijs';

const BrowserUtils = {
    ensureYPosInView: function(yPos: number, footerHeight: number): void {
      var visibleBottom = window.innerHeight + window.scrollY - footerHeight;

      if (yPos > visibleBottom) {
        window.scrollBy(0, yPos - visibleBottom);
      }
    },

    hasQueryParam: function(qpName: string): boolean {
      var url = new URI();
      return url.hasQuery(qpName);
    },

    hasQueryParamWithValue: function(qpName: string, value: any): boolean {
      var url = new URI();
      return url.hasQuery(qpName, value);
    },

    removeQueryParam: function(qpName: string): void {
      var url = new URI();
      url.removeQuery(qpName);
      this.replaceURL(url.href());
    },

    replaceQueryParam: function(qpName: string, value: string): void {
      var url = new URI();
      url.removeQuery(qpName).addQuery(qpName, value);
      this.replaceURL(url.href());
    },

    replaceURL: function(url: string): void {
      window.history.replaceState({}, "", url);
    },

    loadURL: function(url: string): void {
      window.location.href = url;
    }
};

export default BrowserUtils;
