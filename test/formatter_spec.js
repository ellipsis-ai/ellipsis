jest.unmock('../app/assets/javascripts/formatter');

const Formatter = require('../app/assets/javascripts/formatter');

describe("Formatter", () => {
  describe("formatCamelCaseIdentifier", () => {
    it("converts a phrase of words into a camel-case identifier", () => {
      expect(Formatter.formatCamelCaseIdentifier("Todoist add item")).toEqual("todoistAddItem");
    });
    it("strips non-valid characters and extra spaces", () => {
      const human = "The quick  brown FOX — it jumped over the lazy dog!";
      expect(Formatter.formatCamelCaseIdentifier(human)).toEqual("theQuickBrownFOXItJumpedOverTheLazyDog");
    });
    it('ensures the first character is valid', () => {
      expect(Formatter.formatCamelCaseIdentifier('1 for the money')).toEqual("_1ForTheMoney");
    });
  });

  describe("formatList", () => {
    it("returns nothing with an empty list", () => {
      expect(Formatter.formatList([])).toEqual("");
    });

    it("returns the single item with a list of 1", () => {
      expect(Formatter.formatList(["foo"])).toEqual("foo");
    });

    it("combines two items with “and”", () => {
      expect(Formatter.formatList(["a", "b"])).toEqual("a and b");
    });

    it("combines three items with oxford commas", () => {
      expect(Formatter.formatList(["a", "b", "c"])).toEqual("a, b, and c");
    });

    it("combines five items with oxford commas", () => {
      expect(Formatter.formatList(["a", "b", "c", "d", "e"])).toEqual("a, b, c, d, and e");
    });

    it("uses a mapper if provided", () => {
      expect(Formatter.formatList(["a", "b", "c"], (ea) => ea.toUpperCase())).toEqual("A, B, and C");
    });
  });
});
