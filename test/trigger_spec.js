const Trigger = require("../app/assets/javascripts/models/trigger");

describe("Trigger", () => {
  describe("capturesInputIndex", () => {
    it("returns true for 0, false for 1+, when there’s a single capturing paren", () => {
      var t = new Trigger({ isRegex: true, text: "add (.+)"});
      expect(t.capturesInputIndex(0)).toBe(true);
      expect(t.capturesInputIndex(1)).toBe(false);
      expect(t.capturesInputIndex(2)).toBe(false);
    });

    it("returns true for 0 and 1 when there’s at least two capturing parens", () => {
      var t = new Trigger({ isRegex: true, text: "add (.+?) plus (.+)"});
      expect(t.capturesInputIndex(0)).toBe(true);
      expect(t.capturesInputIndex(1)).toBe(true);
      expect(t.capturesInputIndex(2)).toBe(false);
    });

    it("returns false when parens are preceded by backslashes", () => {
      var t = new Trigger({ isRegex: true, text: "add \\(.+?\\)"});
      expect(t.capturesInputIndex(0)).toBe(false);
    });

    it("returns true when capturing parens are the first thing", () => {
      var t = new Trigger({ isRegex: true, text: "(.+?) is good"});
      expect(t.capturesInputIndex(0)).toBe(true);
    });
  });
});
