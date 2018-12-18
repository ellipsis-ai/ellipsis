import * as URI from 'urijs';

const BrowserUtils = {
    ensureYPosInView: function(yPos: number, footerHeight: number): void {
      var visibleBottom = window.innerHeight + window.scrollY - footerHeight;

      if (yPos > visibleBottom) {
        window.scrollBy(0, yPos - visibleBottom);
      }
    },

    getQueryParamValue: function(qpName: string): Option<string> {
      const url = new URI();
      const params = url.query(true) as { [q: string]: string | undefined };
      return params[qpName] || null;
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
      const existingValue = this.getQueryParamValue(qpName);
      if (typeof existingValue === "string") {
        const url = new URI();
        url.removeQuery(qpName);
        this.modifyURL(url.href());
      }
    },

    modifyQueryParam: function(qpName: string, value: string): void {
      const existingValue = this.getQueryParamValue(qpName);
      if (existingValue !== value) {
        const url = new URI();
        url.removeQuery(qpName).addQuery(qpName, value);
        this.modifyURL(url.href());
      }
    },

    replaceURL: function(url: string): void {
      window.history.replaceState({}, "", url);
    },

    modifyURL: function(url: string): void {
      window.history.pushState({}, "", url);
    },

    loadURL: function(url: string): void {
      window.location.href = url;
    }
};

export default BrowserUtils;
