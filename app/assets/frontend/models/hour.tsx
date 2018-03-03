import OptionalInt from './optional_int';

  class Hour extends OptionalInt {
    toString(): string {
      if (this.isPM() && typeof this.value === "number" && this.value > 12) {
        return (this.value - 12).toString();
      } else if (this.isAM() && this.value === 0) {
        return "12";
      } else if (typeof this.value === "number" && this.isValid()) {
        return this.value.toString();
      } else {
        return "";
      }
    }

    isValid(): boolean {
      return typeof this.value === "number" && Hour.isValid(this.value);
    }

    isAM(): boolean {
      return typeof this.value === "number" && Hour.isAM(this.value);
    }

    isPM(): boolean {
      return typeof this.value === "number" && Hour.isPM(this.value);
    }

    convertToAMValue(): number | null {
      return typeof this.value === "number" ? Hour.convertToAM(this.value) : null;
    }

    convertToPMValue(): number | null {
      return typeof this.value === "number" ? Hour.convertToPM(this.value) : null;
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

export default Hour;
