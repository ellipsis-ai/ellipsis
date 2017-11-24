// @flow
define(function(require) {
  const OptionalInt = require('./optional_int');

  class Hour extends OptionalInt {
    toString(): string {
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

    isValid(): boolean {
      return Hour.isValid(this.value);
    }

    isAM(): boolean {
      return Hour.isAM(this.value);
    }

    isPM(): boolean {
      return Hour.isPM(this.value);
    }

    convertToAMValue(): number {
      return Hour.convertToAM(this.value);
    }

    convertToPMValue(): number {
      return Hour.convertToPM(this.value);
    }

    static isValid(hour: number): boolean {
      return Number.isInteger(hour) && hour >= 0 && hour < 24;
    }

    static isAM(hour: number): boolean {
      return Number.isInteger(hour) && hour >= 0 && hour < 12;
    }

    static isPM(hour: number): boolean {
      return Number.isInteger(hour) && hour >= 12 && hour < 24;
    }

    static convertToAM(hour: number): number {
      if (Hour.isPM(hour)) {
        return hour - 12;
      } else {
        return hour;
      }
    }

    static convertToPM(hour: number): number {
      if (Hour.isAM(hour)) {
        return hour + 12;
      } else {
        return hour;
      }
    }

    static fromString(string: string): Hour {
      const parsed = string.substr(-2, 2).match(/^(1[0-2]|[1-9])$/) ||
        string.substr(-1, 1).match(/^([1-9])$/);
      return new Hour(super.fromString(parsed ? parsed[1] : null).value);
    }
  }

  return Hour;
});
