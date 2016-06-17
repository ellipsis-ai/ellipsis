jest.unmock('../app/assets/javascripts/browser_utils');

var BrowserUtils = require('../app/assets/javascripts/browser_utils');

describe('BrowserUtils', () => {
  describe('removeQueryParam', () => {
    beforeEach(() => {
      BrowserUtils.getPathname = jest.fn();
      BrowserUtils.getPathname.mockReturnValue('/edit_behavior/abcdef');
      BrowserUtils.getQueryParams = jest.fn();
      BrowserUtils.setURL = jest.fn();
    });

    it('removes justSaved=true when it’s the only query parameter', () => {
      BrowserUtils.getQueryParams.mockReturnValue('?justSaved=true');
      BrowserUtils.removeQueryParam('justSaved');
      expect(BrowserUtils.setURL.mock.calls[0][0]).toEqual('/edit_behavior/abcdef');
    });

    it('removes justSaved=true but leaves trailing parameters', () => {
      BrowserUtils.getQueryParams.mockReturnValue('?justSaved=true&sky=blue&earth=round');
      BrowserUtils.removeQueryParam('justSaved');
      expect(BrowserUtils.setURL.mock.calls[0][0]).toEqual('/edit_behavior/abcdef?sky=blue&earth=round');
    });

    it('removes justSaved=true but leaves leading parameters', () => {
      BrowserUtils.getQueryParams.mockReturnValue('?sky=blue&earth=round&justSaved=true');
      BrowserUtils.removeQueryParam('justSaved');
      expect(BrowserUtils.setURL.mock.calls[0][0]).toEqual('/edit_behavior/abcdef?sky=blue&earth=round');
    });

    it('removes justSaved=true but leaves leading and trailing parameters', () => {
      BrowserUtils.getQueryParams.mockReturnValue('?sky=blue&justSaved=true&earth=round');
      BrowserUtils.removeQueryParam('justSaved');
      expect(BrowserUtils.setURL.mock.calls[0][0]).toEqual('/edit_behavior/abcdef?sky=blue&earth=round');
    });

    it('doesn’t set the browser URL if justSaved=true isn’t a query parameter', () => {
      BrowserUtils.getQueryParams.mockReturnValue('');
      BrowserUtils.removeQueryParam('justSaved');
      expect(BrowserUtils.setURL.mock.calls.length).toBe(0);
    });
  });
});
