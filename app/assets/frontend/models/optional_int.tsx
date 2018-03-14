class OptionalInt {
  readonly value: number | null;

    constructor(intOrNull: number | null) {
      Object.defineProperties(this, {
        value: {
          value: typeof intOrNull === 'number' && Number.isInteger(intOrNull) ? intOrNull : null,
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

    toString(): string {
      return typeof this.value === "number" && this.isInteger() ? this.value.toString() : "";
    }

    is(callback: (n: number) => boolean): boolean {
      if (typeof this.value === "number" && this.isInteger()) {
        return Boolean(callback(this.value));
      } else {
        return false;
      }
    }

    map(callback: (n: number) => any): any {
      if (typeof this.value === "number" && this.isInteger()) {
        return callback(this.value);
      } else {
        return null;
      }
    }

    isInteger(): boolean {
      return typeof this.value === "number" && Number.isInteger(this.value);
    }

    valueWithinRange(min: number, max: number): number | null {
      if (typeof this.value === "number" && this.isInteger()) {
        return Math.min(Math.max(this.value, min), max);
      } else {
        return null;
      }
    }

    static fromString(string: string) {
      return OptionalInt.fromStringWithDefault(string, null);
    }

    static fromStringWithDefault(string: string, defaultValue: number | null) {
      const result = parseInt(string, 10);
      return new OptionalInt(Number.isInteger(result) ? result : defaultValue);
    }
  }

export default OptionalInt;
