import OptionalInt from './optional_int';
import DayOfWeek from './day_of_week';
import DayOfMonth from './day_of_month';
import Hour from './hour';
import Minute from './minute';
import Month from './month';

interface Time {
  hour: number,
  minute: number
}

export interface RecurrenceJson {
  id: string | null,
  displayString: string | null,
  frequency: number,
  typeName: string,
  timeOfDay: Time | null,
  timeZone: string | null,
  timeZoneName: string | null,
  minuteOfHour: number | null,
  dayOfWeek: number | null,
  dayOfMonth: number | null,
  nthDayOfWeek: number | null,
  month: number | null,
  daysOfWeek: Array<number>
}

interface RecurrenceInterface extends RecurrenceJson {}

class Recurrence implements RecurrenceInterface {
  readonly id: string | null;
  readonly displayString: string | null;
  readonly frequency: number;
  readonly typeName: string;
  readonly timeOfDay: Time | null;
  readonly timeZone: string | null;
  readonly timeZoneName: string | null;
  readonly minuteOfHour: number | null;
  readonly dayOfWeek: number | null;
  readonly dayOfMonth: number | null;
  readonly nthDayOfWeek: number | null;
  readonly month: number | null;
  readonly daysOfWeek: Array<number>;

    constructor(props: Partial<RecurrenceInterface>) {
      const initialProps = Object.assign({
        id: null,
        displayString: "",
        frequency: 1,
        typeName: "",
        timeOfDay: null,
        timeZone: null,
        timeZoneName: null,
        minuteOfHour: null,
        dayOfWeek: null,
        dayOfMonth: null,
        nthDayOfWeek: null,
        month: null,
        daysOfWeek: []
      }, props);

      Object.defineProperties(this, {
        id: { value: initialProps.id, enumerable: true },
        displayString: { value: initialProps.displayString, enumerable: true },
        frequency: { value: initialProps.frequency, enumerable: true },
        typeName: { value: initialProps.typeName, enumerable: true },
        timeOfDay: { value: initialProps.timeOfDay, enumerable: true },
        timeZone: { value: initialProps.timeZone, enumerable: true },
        timeZoneName: { value: initialProps.timeZoneName, enumerable: true },
        minuteOfHour: { value: initialProps.minuteOfHour, enumerable: true },
        dayOfWeek: { value: initialProps.dayOfWeek, enumerable: true },
        dayOfMonth: { value: initialProps.dayOfMonth, enumerable: true },
        nthDayOfWeek: { value: initialProps.nthDayOfWeek, enumerable: true },
        month: { value: initialProps.month, enumerable: true },
        daysOfWeek: { value: initialProps.daysOfWeek, enumerable: true }
      });
    }

    fallbackTimeOfDay(): Time {
      return this.timeOfDay || Recurrence.defaultTimeOfDay();
    }

    forEqualityComparison() {
      return this.clone({
        displayString: null,
        timeZoneName: null
      });
    }

    isValid(): boolean {
      return this.isValidMinutely() || this.isValidHourly() || this.isValidDaily() || this.isValidWeekly() ||
        this.isValidMonthlyByDayOfMonth() || this.isValidMonthlyByNthDayOfWeek() || this.isValidYearly();
    }

    hasValidFrequency(): boolean {
      return new OptionalInt(this.frequency).is(ea => ea > 0);
    }

    hasValidTimeOfDay(): boolean {
      const timeOfDay = this.timeOfDay;
      return Boolean(timeOfDay && this.hasValidTimeZone() && Minute.isValid(timeOfDay.minute) &&
        Hour.isValid(timeOfDay.hour));
    }

    hasValidNthDayOfWeek(): boolean {
      return new OptionalInt(this.nthDayOfWeek).is((ea) => ea >= 1 && ea <= 5);
    }

    hasValidTimeZone(): boolean {
      return Boolean(this.timeZone && this.timeZone.length > 0);
    }

    isValidMinutely(): boolean {
      return this.typeName === "minutely" && this.hasValidFrequency();
    }

    isValidHourly(): boolean {
      return this.typeName === "hourly" && this.hasValidFrequency() && Minute.isValid(this.minuteOfHour);
    }

    isValidDaily(): boolean {
      return this.typeName === "daily" && this.hasValidFrequency() && this.hasValidTimeOfDay();
    }

    isValidWeekly(): boolean {
      return this.typeName === "weekly" && this.hasValidFrequency() && this.hasValidTimeOfDay() &&
        this.daysOfWeek.length > 0 && this.daysOfWeek.every(DayOfWeek.isValid);
    }

    isValidMonthlyByDayOfMonth(): boolean {
      return this.typeName === "monthly_by_day_of_month" && this.hasValidFrequency() && this.hasValidTimeOfDay() &&
        DayOfMonth.isValid(this.dayOfMonth);
    }

    isValidMonthlyByNthDayOfWeek(): boolean {
      return this.typeName === "monthly_by_nth_day_of_week" && this.hasValidFrequency() && this.hasValidTimeOfDay() &&
        this.hasValidNthDayOfWeek() && DayOfWeek.isValid(this.dayOfWeek);
    }

    isValidYearly(): boolean {
      return this.typeName === "yearly" && this.hasValidFrequency() && this.hasValidTimeOfDay() &&
        DayOfMonth.isValid(this.dayOfMonth) && Month.isValid(this.month);
    }

    clone(props: Partial<RecurrenceInterface>): Recurrence {
      return new Recurrence(Object.assign({}, this, props));
    }

    becomeMinutely(): Recurrence {
      return this.clone({
        typeName: "minutely",
        timeOfDay: null,
        timeZone: null,
        timeZoneName: null,
        minuteOfHour: null,
        dayOfWeek: null,
        dayOfMonth: null,
        nthDayOfWeek: null,
        month: null,
        daysOfWeek: []
      });
    }

    becomeHourly(): Recurrence {
      const minuteOfHour = typeof this.minuteOfHour === "number" && Number.isInteger(this.minuteOfHour) ? this.minuteOfHour : 0;
      return this.clone({
        typeName: "hourly",
        minuteOfHour: minuteOfHour,
        timeOfDay: null,
        timeZone: null,
        timeZoneName: null,
        dayOfWeek: null,
        dayOfMonth: null,
        nthDayOfWeek: null,
        month: null,
        daysOfWeek: []
      });
    }

    becomeDaily(defaultProps?: Partial<RecurrenceInterface>): Recurrence {
      return this.clone({
        typeName: "daily",
        timeOfDay: this.fallbackTimeOfDay(),
        minuteOfHour: null,
        dayOfWeek: null,
        dayOfMonth: null,
        nthDayOfWeek: null,
        month: null,
        daysOfWeek: [],
        timeZone: this.timeZone || (defaultProps ? defaultProps.timeZone : null),
        timeZoneName: this.timeZoneName || (defaultProps ? defaultProps.timeZoneName : null)
      });
    }

    becomeWeekly(defaultProps): Recurrence {
      return this.clone({
        typeName: "weekly",
        timeOfDay: this.fallbackTimeOfDay(),
        minuteOfHour: null,
        dayOfWeek: null,
        dayOfMonth: null,
        nthDayOfWeek: null,
        month: null,
        timeZone: this.timeZone || (defaultProps ? defaultProps.timeZone : null),
        timeZoneName: this.timeZoneName || (defaultProps ? defaultProps.timeZoneName : null)
      });
    }

    becomeMonthlyByDayOfMonth(defaultProps): Recurrence {
      return this.clone({
        typeName: "monthly_by_day_of_month",
        timeOfDay: this.fallbackTimeOfDay(),
        minuteOfHour: null,
        dayOfWeek: null,
        nthDayOfWeek: null,
        dayOfMonth: this.dayOfMonth || 1,
        daysOfWeek: [],
        month: null,
        timeZone: this.timeZone || (defaultProps ? defaultProps.timeZone : null),
        timeZoneName: this.timeZoneName || (defaultProps ? defaultProps.timeZoneName : null)
      });
    }

    becomeYearly(defaultProps): Recurrence {
      return this.clone({
        typeName: "yearly",
        timeOfDay: this.fallbackTimeOfDay(),
        minuteOfHour: null,
        dayOfWeek: null,
        nthDayOfWeek: null,
        daysOfWeek: [],
        dayOfMonth: this.dayOfMonth || 1,
        month: 1,
        timeZone: this.timeZone || (defaultProps ? defaultProps.timeZone : null),
        timeZoneName: this.timeZoneName || (defaultProps ? defaultProps.timeZoneName : null)
      });
    }

    static defaultTimeOfDay(): Time {
      return { hour: 9, minute: 0 };
    }
  }

export default Recurrence;
