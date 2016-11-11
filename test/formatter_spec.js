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
});
