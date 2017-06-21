define(function(require) {

  const OptionalInt = require('../models/optional_int'),
    DayOfWeek = require('../models/day_of_week'),
    DayOfMonth = require('../models/day_of_month'),
    Hour = require('../models/hour'),
    Minute = require('../models/minute'),
    Month = require('../models/month');

  class Recurrence {

    constructor(props) {
      const initialProps = Object.assign({
        id: null,
        displayString: "",
        frequency: 1,
        typeName: "daily",
        timeOfDay: Recurrence.defaultTimeOfDay(),
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

    fallbackTimeOfDay() {
      return this.timeOfDay || Recurrence.defaultTimeOfDay();
    }

    forEqualityComparison() {
      return this.clone({
        displayString: null,
        timeZoneName: null
      });
    }

    isValid() {
      return this.isValidMinutely() || this.isValidHourly() || this.isValidDaily() || this.isValidWeekly() ||
        this.isValidMonthlyByDayOfMonth() || this.isValidMonthlyByNthDayOfWeek() || this.isValidYearly();
    }

    hasValidFrequency() {
      return new OptionalInt(this.frequency).is(ea => ea > 0);
    }

    hasValidTimeOfDay() {
      return this.timeOfDay && this.hasValidTimeZone() && Minute.isValid(this.timeOfDay.minute) &&
        Hour.isValid(this.timeOfDay.hour);
    }

    hasValidNthDayOfWeek() {
      return new OptionalInt(this.nthDayOfWeek).is((ea) => ea >= 1 && ea <= 5);
    }

    hasValidTimeZone() {
      return this.timeZone.length > 0;
    }

    isValidMinutely() {
      return this.typeName === "minutely" && this.hasValidFrequency();
    }

    isValidHourly() {
      return this.typeName === "hourly" && this.hasValidFrequency() && Hour.isValid(this.minuteOfHour);
    }

    isValidDaily() {
      return this.typeName === "daily" && this.hasValidFrequency() && this.hasValidTimeOfDay();
    }

    isValidWeekly() {
      return this.typeName === "weekly" && this.hasValidFrequency() && this.hasValidTimeOfDay() &&
        this.daysOfWeek.length > 0 && this.daysOfWeek.every(DayOfWeek.isValid);
    }

    isValidMonthlyByDayOfMonth() {
      return this.typeName === "monthly_by_day_of_month" && this.hasValidFrequency() && this.hasValidTimeOfDay() &&
        DayOfMonth.isValid(this.dayOfMonth);
    }

    isValidMonthlyByNthDayOfWeek() {
      return this.typeName === "monthly_by_nth_day_of_week" && this.hasValidFrequency() && this.hasValidTimeOfDay() &&
        this.hasValidNthDayOfWeek() && DayOfWeek.isValid(this.dayOfWeek);
    }

    isValidYearly() {
      return this.typeName === "yearly" && this.hasValidFrequency() && this.hasValidTimeOfDay() &&
        DayOfMonth.isValid(this.dayOfMonth) && Month.isValid(this.month);
    }

    clone(props) {
      return new Recurrence(Object.assign({}, this, props));
    }

    becomeMinutely() {
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

    becomeHourly() {
      const minuteOfHour = Number.isInteger(this.minuteOfHour) ? this.minuteOfHour : 0;
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

    becomeDaily(defaultProps) {
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

    becomeWeekly(defaultProps) {
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

    becomeMonthlyByDayOfMonth(defaultProps) {
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

    becomeYearly(defaultProps) {
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

    static defaultTimeOfDay() {
      return { hour: 9, minute: 0 };
    }
  }

  return Recurrence;
});
