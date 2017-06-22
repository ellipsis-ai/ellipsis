define(function(require) {
  const OptionalInt = require('./optional_int');

  class Minute extends OptionalInt {
    toString() {
      if (this.isInteger()) {
        return this.value.toString().padStart(2, "0");
      } else {
        return super.toString();
      }
    }

    static fromString(string) {
      const minutesRegex = /^([0-5]?[0-9])$/;
      const parsed = string.substr(-2, 2).match(minutesRegex) ||
        (string.substr(-3, 1) + string.substr(-1, 1)).match(minutesRegex);
      return new Minute(super.fromString(parsed ? parsed[1] : "").value);
    }

    static isValid(intOrNull) {
      const m = new Minute(intOrNull);
      return m.is((ea) => ea >= 0 && ea <= 59);
    }
  }

  return Minute;
});
