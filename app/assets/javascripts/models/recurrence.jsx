define(function() {

  class Recurrence {

    constructor(props) {
      const initialProps = Object.assign({
        displayString: "",
        frequency: 1,
        typeName: "daily",
        timeOfDay: {
          hour: 9,
          minute: 0
        },
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
      return this.clone({
        typeName: "hourly",
        timeOfDay: null,
        timeZone: null,
        dayOfWeek: null,
        dayOfMonth: null,
        nthDayOfWeek: null,
        month: null,
        daysOfWeek: []
      });
    }

    becomeDaily() {
      return this.clone({
        typeName: "daily",
        minuteOfHour: null,
        dayOfWeek: null,
        dayOfMonth: null,
        nthDayOfWeek: null,
        month: null,
        daysOfWeek: []
      });
    }

    becomeWeekly() {
      return this.clone({
        typeName: "weekly",
        minuteOfHour: null,
        dayOfWeek: null,
        dayOfMonth: null,
        nthDayOfWeek: null,
        month: null
      });
    }

    becomeMonthly() {
      return this.clone({
        typeName: "monthly",
        minuteOfHour: null,
        daysOfWeek: [],
        month: null
      });
    }

    becomeYearly() {
      return this.clone({
        typeName: "yearly",
        minuteOfHour: null,
        dayOfWeek: null,
        nthDayOfWeek: null,
        daysOfWeek: []
      });
    }
  }

  return Recurrence;
});
