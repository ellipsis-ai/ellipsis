define(function() {

  class Recurrence {

    constructor(props) {
      const initialProps = Object.assign({
        displayString: "",
        frequency: 1,
        typeName: "daily",
        timeOfDay: Recurrence.defaultTimeOfDay(),
        timeZone: null,
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
        minuteOfHour: null,
        dayOfWeek: null,
        dayOfMonth: null,
        nthDayOfWeek: null,
        month: null,
        daysOfWeek: []
      });
    }

    becomeHourly() {
      let minuteOfHour;
      if (typeof(this.minuteOfHour) === "number") {
        minuteOfHour = this.minuteOfHour;
      } else {
        minuteOfHour = 0;
      }
      return this.clone({
        typeName: "hourly",
        minuteOfHour: minuteOfHour,
        timeOfDay: null,
        timeZone: null,
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
        timeZone: this.timeZone || (defaultProps ? defaultProps.timeZone : null)
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
        timeZone: this.timeZone || (defaultProps ? defaultProps.timeZone : null)
      });
    }

    becomeMonthly(defaultProps) {
      return this.clone({
        typeName: "monthly",
        timeOfDay: this.fallbackTimeOfDay(),
        minuteOfHour: null,
        dayOfWeek: this.dayOfWeek || null,
        nthDayOfWeek: this.nthDayOfWeek || typeof(this.dayOfWeek) === "number" ? 1 : null,
        dayOfMonth: typeof(this.dayOfWeek) === "number" ? null : 1,
        daysOfWeek: [],
        month: null,
        timeZone: this.timeZone || (defaultProps ? defaultProps.timeZone : null)
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
        timeZone: this.timeZone || (defaultProps ? defaultProps.timeZone : null)
      });
    }

    static defaultTimeOfDay() {
      return { hour: 9, minute: 0 };
    }
  }

  return Recurrence;
});
