"use strict";

jest.unmock('../index');

const RR = require('../index');

describe("RandomResponse", () => {
  describe("emoji", () => {
    it("returns a happy emoji", () => {
      const happyEmoji = RR.emoji('happy');
      expect(happyEmoji).toBeTruthy();
      expect(RR.emojiListFor('happy').some((ea) => ea === happyEmoji)).toBe(true);
    });
  });

  describe("response", () => {
    it("returns a happy response", () => {
      const happyResponse = RR.response('happy');
      expect(happyResponse).toBeTruthy();
      expect(RR.responseListFor('happy').some((ea) => ea === happyResponse)).toBe(true);
    });
  });

  describe("responseWithEmoji", () => {
    it("returns a happy emoji + response", () => {
      const happyCombo = RR.responseWithEmoji('happy');
      expect(happyCombo).toBeTruthy();
      expect(RR.emojiListFor('happy').some((ea) => happyCombo.indexOf(ea) === 0)).toBe(true);
      expect(RR.responseListFor('happy').some((ea) => happyCombo.indexOf(ea) > 0)).toBe(true);
    });
  });
});
