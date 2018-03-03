import OptionalInt from './optional_int';

  class Month extends OptionalInt {
    static NAMES: Array<string>;
    static SHORT_NAMES: Array<string>;
    static MAX_DAYS: Array<number>;
    static JANUARY: Month;
    static FEBRUARY: Month;
    static MARCH: Month;
    static APRIL: Month;
    static MAY: Month;
    static JUNE: Month;
    static JULY: Month;
    static AUGUST: Month;
    static SEPTEMBER: Month;
    static OCTOBER: Month;
    static NOVEMBER: Month;
    static DECEMBER: Month;
    static YEAR: Array<Month>;

    name(): string {
      return typeof this.value === "number" && this.isValid() ? Month.NAMES[this.value - 1] : "";
    }

    shortName(): string {
      return typeof this.value === "number" && this.isValid() ? Month.SHORT_NAMES[this.value - 1] : "";
    }

    maxDays(): number | null {
      return typeof this.value === "number" && this.isValid() ? Month.MAX_DAYS[this.value - 1] : null;
    }

    limitDaytoMax(day: number): number | null {
      const days = this.maxDays();
      return days ? Math.min(days, day) : day;
    }

    isValid(): boolean {
      return this.is((int) => int >= 1 && int <= 12);
    }

    static fromString(string): Month {
      const parsed = string.match(/^(1[0-2]|[1-9])$/);
      const int = super.fromStringWithDefault(parsed ? parsed[1] : "", Month.JANUARY.value);
      return new Month(int.value);
    }

    static isValid(intOrNull): boolean {
      return new Month(intOrNull).isValid();
    }
  }

  Object.defineProperties(Month, {
    NAMES: {
      value: ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"],
      enumerable: false
    },
    SHORT_NAMES: {
      value: ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"],
      enumerable: false
    },
    MAX_DAYS: {
      value: [31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31],
      enumerable: false
    }
  });

  Month.JANUARY   = new Month(1);
  Month.FEBRUARY  = new Month(2);
  Month.MARCH     = new Month(3);
  Month.APRIL     = new Month(4);
  Month.MAY       = new Month(5);
  Month.JUNE      = new Month(6);
  Month.JULY      = new Month(7);
  Month.AUGUST    = new Month(8);
  Month.SEPTEMBER = new Month(9);
  Month.OCTOBER   = new Month(10);
  Month.NOVEMBER  = new Month(11);
  Month.DECEMBER  = new Month(12);
  Month.YEAR = [
    Month.JANUARY,
    Month.FEBRUARY,
    Month.MARCH,
    Month.APRIL,
    Month.MAY,
    Month.JUNE,
    Month.JULY,
    Month.AUGUST,
    Month.SEPTEMBER,
    Month.OCTOBER,
    Month.NOVEMBER,
    Month.DECEMBER
  ];

export default Month;
