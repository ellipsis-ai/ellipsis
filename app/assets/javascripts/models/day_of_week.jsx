define(function(require) {
  const OptionalInt = require('./optional_int');

  class DayOfWeek extends OptionalInt {
    name() {
      return DayOfWeek.NAMES[this.value] || "";
    }

    shortName() {
      return DayOfWeek.SHORT_NAMES[this.value] || "";
    }

    static fromString(string) {
      const parsed = string.match(/^([0-6])$/);
      const int = super.fromStringWithDefault(parsed ? parsed[1] : "", DayOfWeek.MONDAY.value);
      return new DayOfWeek(int.value);
    }

    static isValid(intOrNull) {
      const d = new DayOfWeek(intOrNull);
      return d.is((ea) => ea >= 0 && ea <= 6);
    }
  }

  Object.defineProperties(DayOfWeek, {
    NAMES: {
      value: ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"],
      enumerable: false
    },
    SHORT_NAMES: {
      value: ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"],
      enumerable: false
    }
  });

  DayOfWeek.SUNDAY = new DayOfWeek(0);
  DayOfWeek.MONDAY = new DayOfWeek(1);
  DayOfWeek.TUESDAY = new DayOfWeek(2);
  DayOfWeek.WEDNESDAY = new DayOfWeek(3);
  DayOfWeek.THURSDAY = new DayOfWeek(4);
  DayOfWeek.FRIDAY = new DayOfWeek(5);
  DayOfWeek.SATURDAY = new DayOfWeek(6);
  DayOfWeek.WEEK = [
    DayOfWeek.MONDAY,
    DayOfWeek.TUESDAY,
    DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY,
    DayOfWeek.SATURDAY,
    DayOfWeek.SUNDAY
  ];

  return DayOfWeek;
});
