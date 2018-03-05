import OptionalInt from './optional_int';

  class Minute extends OptionalInt {
    toString() {
      if (typeof this.value === "number" && this.isInteger()) {
        return this.value.toString().replace(/^(\d)$/, "0$1");
      } else {
        return super.toString();
      }
    }

    static fromString(string): Minute {
      const minutesRegex = /^([0-5]?[0-9])$/;
      const parsed = string.substr(-2, 2).match(minutesRegex) ||
        (string.substr(-3, 1) + string.substr(-1, 1)).match(minutesRegex);
      return new Minute(super.fromString(parsed ? parsed[1] : "").value);
    }

    static isValid(intOrNull: number | null): boolean {
      const m = new Minute(intOrNull);
      return m.is((ea) => ea >= 0 && ea <= 59);
    }
  }

export default Minute;
