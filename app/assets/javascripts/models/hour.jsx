define(function(require) {
  const OptionalInt = require('./optional_int');

  class Hour extends OptionalInt {
    toString() {
      if (this.isPM() && this.value > 12) {
        return (this.value - 12).toString();
      } else if (this.isAM() && this.value === 0) {
        return "12";
      } else if (this.isValid()) {
        return this.value.toString();
      } else {
        return "";
      }
    }

    isValid() {
      return Hour.isValid(this.value);
    }

    isAM() {
      return Hour.isAM(this.value);
    }

    isPM() {
      return Hour.isPM(this.value);
    }

    convertToAMValue() {
      return Hour.convertToAM(this.value);
    }

    convertToPMValue() {
      return Hour.convertToPM(this.value);
    }

    static isValid(hour) {
      return Number.isInteger(hour) && hour >= 0 && hour < 24;
    }

    static isAM(hour) {
      return Number.isInteger(hour) && hour >= 0 && hour < 12;
    }

    static isPM(hour) {
      return Number.isInteger(hour) && hour >= 12 && hour < 24;
    }

    static convertToAM(hour) {
      if (Hour.isPM(hour)) {
        return hour - 12;
      } else {
        return hour;
      }
    }

    static convertToPM(hour) {
      if (Hour.isAM(hour)) {
        return hour + 12;
      } else {
        return hour;
      }
    }

    static fromString(string) {
      const parsed = string.substr(-2, 2).match(/^(1[0-2]|[1-9])$/) ||
        string.substr(-1, 1).match(/^([1-9])$/);
      return new Hour(super.fromString(parsed ? parsed[1] : null).value);
    }
  }

  return Hour;
});
