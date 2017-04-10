"use strict";

let mockedHour = jest.fn(() => 0);

jest.mock("moment-timezone", () => (function() {
  const mock = {
    tz: () => mock,
    hour: () => mockedHour()
  };
  return mock;
}));

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

  describe("greetingForTimeZone", () => {
    it("returns a hello response if no time zone is included", () => {
      const noTimeZone = RR.greetingForTimeZone();
      expect(noTimeZone).toBeTruthy();
      expect(RR.emojiListFor('hello').some((ea) => noTimeZone.indexOf(ea) === 0)).toBe(true);
      expect(RR.responseListFor('hello').some((ea) => noTimeZone.indexOf(ea) > 0)).toBe(true);
    });

    it("returns a morning response in the morning", () => {
      mockedHour = jest.fn(() => 6);
      const morning = RR.greetingForTimeZone('America/Los_Angeles');
      expect(morning).toBeTruthy();
      expect(RR.emojiListFor('good_morning').some((ea) => morning.indexOf(ea) === 0)).toBe(true);
      expect(RR.responseListFor('good_morning').some((ea) => morning.indexOf(ea) > 0)).toBe(true);
    });

    it("returns an afternoon response in the afternoon", () => {
      mockedHour = jest.fn(() => 12);
      const afternoon = RR.greetingForTimeZone('America/Los_Angeles');
      expect(afternoon).toBeTruthy();
      expect(RR.emojiListFor('good_afternoon').some((ea) => afternoon.indexOf(ea) === 0)).toBe(true);
      expect(RR.responseListFor('good_afternoon').some((ea) => afternoon.indexOf(ea) > 0)).toBe(true);
    });

    it("returns an evening response in the evening", () => {
      mockedHour = jest.fn(() => 19);
      const evening = RR.greetingForTimeZone('America/Los_Angeles');
      expect(evening).toBeTruthy();
      expect(RR.emojiListFor('good_evening').some((ea) => evening.indexOf(ea) === 0)).toBe(true);
      expect(RR.responseListFor('good_evening').some((ea) => evening.indexOf(ea) > 0)).toBe(true);
    });

    it("returns a late night response otherwise", () => {
      mockedHour = jest.fn(() => 23);
      const night = RR.greetingForTimeZone('America/Los_Angeles');
      expect(night).toBeTruthy();
      expect(RR.emojiListFor('late_night').some((ea) => night.indexOf(ea) === 0)).toBe(true);
      expect(RR.responseListFor('late_night').some((ea) => night.indexOf(ea) > 0)).toBe(true);

      mockedHour = jest.fn(() => 3);
      const night2 = RR.greetingForTimeZone('America/Los_Angeles');
      expect(night2).toBeTruthy();
      expect(RR.emojiListFor('late_night').some((ea) => night2.indexOf(ea) === 0)).toBe(true);
      expect(RR.responseListFor('late_night').some((ea) => night2.indexOf(ea) > 0)).toBe(true);
    });
  });
});
