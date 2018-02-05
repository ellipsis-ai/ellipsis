// @flow
import OptionalInt from './optional_int';

  class DayOfWeek extends OptionalInt {
    name(): string {
      return DayOfWeek.NAMES[this.value - 1] || "";
    }

    shortName(): string {
      return DayOfWeek.SHORT_NAMES[this.value - 1] || "";
    }

    static fromString(string): DayOfWeek {
      const parsed = string.match(/^([1-7])$/);
      const int = super.fromStringWithDefault(parsed ? parsed[1] : "", DayOfWeek.MONDAY.value);
      return new DayOfWeek(int.value);
    }

    static isValid(intOrNull): boolean {
      const d = new DayOfWeek(intOrNull);
      return d.is((ea) => ea >= 1 && ea <= 7);
    }
  }

  Object.defineProperties(DayOfWeek, {
    NAMES: {
      value: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"],
      enumerable: false
    },
    SHORT_NAMES: {
      value: ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"],
      enumerable: false
    }
  });

  DayOfWeek.MONDAY = new DayOfWeek(1);
  DayOfWeek.TUESDAY = new DayOfWeek(2);
  DayOfWeek.WEDNESDAY = new DayOfWeek(3);
  DayOfWeek.THURSDAY = new DayOfWeek(4);
  DayOfWeek.FRIDAY = new DayOfWeek(5);
  DayOfWeek.SATURDAY = new DayOfWeek(6);
  DayOfWeek.SUNDAY = new DayOfWeek(7);
  DayOfWeek.WEEK = [
    DayOfWeek.MONDAY,
    DayOfWeek.TUESDAY,
    DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY,
    DayOfWeek.SATURDAY,
    DayOfWeek.SUNDAY
  ];

export default DayOfWeek;
