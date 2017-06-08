define(function(require) {
  const OptionalInt = require('./optional_int');

  class DayOfMonth extends OptionalInt {
    ordinalSuffix() {
      const lastDigit = this.value % 10;
      const last2Digits = this.value % 100;
      if (lastDigit === 1 && last2Digits !== 11) {
        return "st";
      } else if (lastDigit === 2 && last2Digits !== 12) {
        return "nd";
      } else if (lastDigit === 3 && last2Digits !== 13) {
        return "rd";
      } else {
        return "th";
      }
    }

    static fromString(string) {
      const parsed = string.substr(-2, 2).match(/(3[0-1]|[1-2][0-9]|[1-9])$/);
      return new DayOfMonth(super.fromString(parsed ? parsed[1] : "").value);
    }
  }

  return DayOfMonth;
});
