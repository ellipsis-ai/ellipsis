class OptionalInt {
    value: number | null;

    constructor(intOrNull) {
      Object.defineProperties(this, {
        value: {
          value: Number.isInteger(intOrNull) ? intOrNull : null,
          enumerable: true
        }
      });
    }

    valueOf() {
      return this.value;
    }

    toJSON() {
      return this.value;
    }

    toString() {
      return typeof this.value === "number" && this.isInteger() ? this.value.toString() : "";
    }

    is(callback) {
      if (this.isInteger()) {
        return Boolean(callback(this.value));
      } else {
        return false;
      }
    }

    map(callback: (n: number) => any): any | null {
      if (typeof this.value === "number" && this.isInteger()) {
        return callback(this.value);
      } else {
        return null;
      }
    }

    isInteger() {
      return typeof this.value === "number" && Number.isInteger(this.value);
    }

    valueWithinRange(min, max) {
      if (this.isInteger() && this.value) {
        return Math.min(Math.max(this.value, min), max);
      } else {
        return null;
      }
    }

    static fromString(string) {
      return OptionalInt.fromStringWithDefault(string, null);
    }

    static fromStringWithDefault(string, defaultValue) {
      const result = parseInt(string, 10);
      return new OptionalInt(Number.isInteger(result) ? result : defaultValue);
    }
  }

export default OptionalInt;
