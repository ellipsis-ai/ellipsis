define(function() {

  class Recurrence {

    constructor(props) {
      const initialProps = Object.assign({
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
