import OptionalInt from './optional_int';

  class DayOfMonth extends OptionalInt {
    ordinalSuffix() {
      const lastDigit = this.map(v => v % 10);
      const last2Digits = this.map(v => v % 100);
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

    isValid(): boolean {
      return DayOfMonth.isValid(this.value);
    }

    static fromString(string: string): DayOfMonth {
      const parsed = string.substr(-2, 2).match(/(3[0-1]|[1-2][0-9]|[1-9])$/);
      return new DayOfMonth(super.fromString(parsed ? parsed[1] : "").value);
    }

    static isValid(intOrNull: Option<number>): boolean {
      return new DayOfMonth(intOrNull).is((ea) => ea >= 1 && ea <= 31);
    }
  }

export default DayOfMonth;
