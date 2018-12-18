import BrowserUtils from "../../../../app/assets/frontend/lib/browser_utils";

let location: string;
let history: Array<string>;

function reset() {
  location = 'http://start.start/';
  history = [location];
}

window.history.replaceState = jest.fn((data, title, newUrl: string) => {
  location = newUrl;
});
window.history.pushState = jest.fn((data, title, newUrl: string) => {
  location = newUrl;
  history.push(newUrl);
});

jest.mock('urijs', () => {
  // Super dumb mock that only lets you add/remove/check "?foo=bar"
  class URI {
    query() {
      return /\?foo=bar/.test(location) ? { foo: "bar" } : {};
    }

    hasQuery() {
      return Boolean(this.query().foo)
    }

    removeQuery() {
      location = location.replace(/\?foo=bar$/, "");
      return this;
    }

    addQuery() {
      location = location + '?foo=bar';
      return this;
    }

    href() {
      return location;
    }
  }
  return URI;
});

describe("BrowserUtils", () => {

  beforeEach(() => {
    jest.clearAllMocks();
    reset();
  });

  describe("replaceURL", () => {
    it('replaces the existing browser history state without adding to it', () => {
      BrowserUtils.replaceURL("http://hahaha.com");
      expect(window.history.replaceState).toHaveBeenCalled();
      expect(window.history.pushState).not.toHaveBeenCalled();
      expect(history).toHaveLength(1);
    });
  });

  describe("modifyUrl", () => {
    it('adds a new entry to the browser history state', () => {
      BrowserUtils.modifyURL("http://hahaha.com");
      expect(window.history.pushState).toHaveBeenCalled();
      expect(window.history.replaceState).not.toHaveBeenCalled();
      expect(history).toHaveLength(2);
    });
  });

  describe("modifyQueryParam", () => {
    it('modifies the URL only if a query parameter is not already set to that value', () => {
      BrowserUtils.modifyQueryParam("foo", "bar");
      BrowserUtils.modifyQueryParam("foo", "bar");
      expect(window.history.pushState).toHaveBeenCalledTimes(1);
      expect(history).toHaveLength(2);
    });
  });
});
