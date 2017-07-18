const Formatter = require('../app/assets/javascripts/lib/formatter');

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

  describe("formatNameForCode", () => {
    it("satisfies the requirements of JS and GraphQL symbol identifiers", () => {
      const result = Formatter.formatNameForCode("@@ jksfhla skjgho 9#$@ kjshf 🤔 kljsfghk L#O&*@Y jkhs lkjh");
      expect(/[_A-Za-z][_0-9A-Za-z]*/.test(result)).toBe(true);
    });

    it("strips leading numbers", () => {
      expect(Formatter.formatNameForCode("1abc")).toEqual("abc");
    });

    it("adds an underscore prefix reserved words", () => {
      expect(Formatter.formatNameForCode("for")).toEqual("_for");
    });

    it("removes an underscore prefix when one more character is typed after a reserved word", () => {
      expect(Formatter.formatNameForCode("_form")).toEqual("form");
    });

    it("preserves underscore prefixes for strings that start with a reserved word with 2 or more characters", () => {
      expect(Formatter.formatNameForCode("_format")).toEqual("_format");
    });

    it("strips more than a single underscore prefix", () => {
      expect(Formatter.formatNameForCode("__hmm")).toEqual("_hmm");
      expect(Formatter.formatNameForCode("____hmm")).toEqual("_hmm");
    });
  });

  describe("formatPossibleNumber", () => {
    it("strips non-numeric characters", () => {
      expect(Formatter.formatPossibleNumber("")).toEqual("");
      expect(Formatter.formatPossibleNumber("abc∂é")).toEqual("");
      expect(Formatter.formatPossibleNumber("01abc23")).toEqual("123");
    });

    it("preserves leading negative and first decimal", () => {
      expect(Formatter.formatPossibleNumber("-123.4")).toEqual("-123.4");
      expect(Formatter.formatPossibleNumber(".")).toEqual(".");
      expect(Formatter.formatPossibleNumber(".5")).toEqual(".5");
      expect(Formatter.formatPossibleNumber(" 1.5")).toEqual("1.5");
      expect(Formatter.formatPossibleNumber("$0.67.9 A-B-C")).toEqual("0.679");
    });

    it("strips redundant zeros", () => {
      expect(Formatter.formatPossibleNumber("0")).toEqual("0");
      expect(Formatter.formatPossibleNumber("0.0")).toEqual("0.0");
      expect(Formatter.formatPossibleNumber("0.00")).toEqual("0.00");
      expect(Formatter.formatPossibleNumber("00")).toEqual("0");
      expect(Formatter.formatPossibleNumber("00.0")).toEqual("0.0");
      expect(Formatter.formatPossibleNumber("01")).toEqual("1");
      expect(Formatter.formatPossibleNumber("0.1")).toEqual("0.1");
      expect(Formatter.formatPossibleNumber("0.10")).toEqual("0.10");
      expect(Formatter.formatPossibleNumber("0000.010000")).toEqual("0.010000");
    });
  });
});
