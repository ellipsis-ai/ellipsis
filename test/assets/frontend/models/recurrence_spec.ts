import Recurrence from '../../../../app/assets/frontend/models/recurrence';

describe('Recurrence', () => {

  const defaults = Object.freeze({
    id: null,
    displayString: "",
    frequency: null,
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
  });

  function newRecurrence(props) {
    return new Recurrence(Object.assign({}, defaults, props));
  }

  describe('minutely', () => {
    it('returns isValid true with a valid minutely recurrence', () => {

      const validMinutely = newRecurrence({
        frequency: 1,
        typeName: "minutely"
      });
      expect(validMinutely.isValid()).toBe(true);

    });

    it('returns isValid false with an invalid minutely recurrence', () => {
      const invalidMinutely = newRecurrence({
        frequency: 0,
        typeName: "minutely"
      });

      expect(invalidMinutely.isValid()).toBe(false);
    });

  });

  describe('hourly', () => {

    it('returns isValid true with valid hourly recurrences', () => {
      const validHourly1 = newRecurrence({
        frequency: 1,
        typeName: "hourly",
        minuteOfHour: 0
      });

      const validHourly2 = newRecurrence({
        frequency: 1,
        typeName: "hourly",
        minuteOfHour: 35
      });

      const validHourly3 = newRecurrence({
        frequency: 1,
        typeName: "hourly",
        minuteOfHour: 59
      });

      expect(validHourly1.isValid()).toBe(true);
      expect(validHourly2.isValid()).toBe(true);
      expect(validHourly3.isValid()).toBe(true);
    });

    it('returns isValid false with invalid hourly recurrences', () => {
      const invalidMinuteOfHour = newRecurrence({
        frequency: 1,
        typeName: "hourly",
        minuteOfHour: 60
      });

      const invalidHourlyFrequency = newRecurrence({
        frequency: 0,
        typeName: "hourly",
        minuteOfHour: 59
      });

      expect(invalidMinuteOfHour.isValid()).toBe(false);
      expect(invalidHourlyFrequency.isValid()).toBe(false);
    });
  });

  describe('daily', () => {

    it('returns isValid true with valid daily recurrences', () => {
      const validDaily1 = newRecurrence({
        frequency: 1,
        typeName: "daily",
        timeOfDay: {
          hour: 0,
          minute: 0
        },
        timeZone: "America/Los_Angeles"
      });

      const validDaily2 = newRecurrence({
        frequency: 2,
        typeName: "daily",
        timeOfDay: {
          hour: 12,
          minute: 59
        },
        timeZone: "America/Los_Angeles"
      });

      const validDaily3 = newRecurrence({
        frequency: 5,
        typeName: "daily",
        timeOfDay: {
          hour: 23,
          minute: 30
        },
        timeZone: "America/Los_Angeles"
      });

      expect(validDaily1.isValid()).toBe(true);
      expect(validDaily2.isValid()).toBe(true);
      expect(validDaily3.isValid()).toBe(true);
    });

    it('returns isValid false with invalid daily recurrences', () => {

      const invalidDailyFrequency = newRecurrence({
        frequency: 0,
        typeName: "daily",
        timeOfDay: {
          hour: 12,
          minute: 0
        },
        timeZone: "America/Los_Angeles"
      });

      const invalidDailyHour = newRecurrence({
        frequency: 1,
        typeName: "daily",
        timeOfDay: {
          hour: 24,
          minute: 0
        },
        timeZone: "America/Los_Angeles"
      });

      const invalidDailyMinute = newRecurrence({
        frequency: 1,
        typeName: "daily",
        timeOfDay: {
          hour: 23,
          minute: 60
        },
        timeZone: "America/Los_Angeles"
      });

      const invalidDailyTz = newRecurrence({
        frequency: 1,
        typeName: "daily",
        timeOfDay: {
          hour: 0,
          minute: 0
        },
        timeZone: null
      });

      expect(invalidDailyFrequency.isValid()).toBe(false);
      expect(invalidDailyHour.isValid()).toBe(false);
      expect(invalidDailyMinute.isValid()).toBe(false);
      expect(invalidDailyTz.isValid()).toBe(false);
    });
  });

  describe('weekly', () => {
    it('returns isValid true for valid weekly recurrences', () => {
      const weekly1 = newRecurrence({
        frequency: 1,
        typeName: "weekly",
        timeOfDay: {
          hour: 0,
          minute: 0
        },
        daysOfWeek: [1, 2, 3, 4, 5, 6, 7],
        timeZone: "America/Los_Angeles"
      });

      const weekly2 = newRecurrence({
        frequency: 5,
        typeName: "weekly",
        timeOfDay: {
          hour: 12,
          minute: 30
        },
        daysOfWeek: [1],
        timeZone: "America/Los_Angeles"
      });

      const weekly3 = newRecurrence({
        frequency: 10,
        typeName: "weekly",
        timeOfDay: {
          hour: 23,
          minute: 59
        },
        daysOfWeek: [7],
        timeZone: "America/Los_Angeles"
      });

      expect(weekly1.isValid()).toBe(true);
      expect(weekly2.isValid()).toBe(true);
      expect(weekly3.isValid()).toBe(true);

    });

    it('returns isValid false for invalid weekly recurrences', () => {
      const invalidWeeklyFrequency = newRecurrence({
        frequency: 0,
        typeName: "weekly",
        timeOfDay: {
          hour: 0,
          minute: 0
        },
        daysOfWeek: [1, 2, 3, 4, 5, 6, 7],
        timeZone: "America/Los_Angeles"
      });

      const invalidWeeklyTime = newRecurrence({
        frequency: 1,
        typeName: "weekly",
        timeOfDay: {
          hour: 24,
          minute: 0
        },
        daysOfWeek: [1, 2, 3, 4, 5, 6, 7],
        timeZone: "America/Los_Angeles"
      });

      const invalidWeeklyDays = newRecurrence({
        frequency: 0,
        typeName: "weekly",
        timeOfDay: {
          hour: 0,
          minute: 0
        },
        daysOfWeek: [0, 1, 2, 3, 4, 5, 6],
        timeZone: "America/Los_Angeles"
      });

      const invalidWeeklyDaysMissing = newRecurrence({
        frequency: 0,
        typeName: "weekly",
        timeOfDay: {
          hour: 0,
          minute: 0
        },
        daysOfWeek: [],
        timeZone: "America/Los_Angeles"
      });

      const invalidWeeklyTz = newRecurrence({
        frequency: 0,
        typeName: "weekly",
        timeOfDay: {
          hour: 0,
          minute: 0
        },
        daysOfWeek: [1, 2, 3, 4, 5, 6, 7],
        timeZone: ""
      });

      expect(invalidWeeklyFrequency.isValid()).toBe(false);
      expect(invalidWeeklyTime.isValid()).toBe(false);
      expect(invalidWeeklyDays.isValid()).toBe(false);
      expect(invalidWeeklyDaysMissing.isValid()).toBe(false);
      expect(invalidWeeklyTz.isValid()).toBe(false);
    });
  });

  describe('monthly by day of month', () => {
    it('returns isValid true for valid monthly by day of month recurrences', () => {
      const monthly1 = newRecurrence({
        frequency: 1,
        typeName: "monthly_by_day_of_month",
        timeOfDay: {
          hour: 0,
          minute: 0
        },
        dayOfMonth: 1,
        timeZone: "America/Los_Angeles"
      });

      const monthly2 = newRecurrence({
        frequency: 5,
        typeName: "monthly_by_day_of_month",
        timeOfDay: {
          hour: 12,
          minute: 30
        },
        dayOfMonth: 15,
        timeZone: "America/Los_Angeles"
      });

      const monthly3 = newRecurrence({
        frequency: 10,
        typeName: "monthly_by_day_of_month",
        timeOfDay: {
          hour: 23,
          minute: 59
        },
        dayOfMonth: 31,
        timeZone: "America/Los_Angeles"
      });

      expect(monthly1.isValid()).toBe(true);
      expect(monthly2.isValid()).toBe(true);
      expect(monthly3.isValid()).toBe(true);
    });

    it('returns isValid false for invalid monthly by day of month recurrences', () => {
      const invalidMonthlyFrequency = newRecurrence({
        frequency: 0,
        typeName: "monthly_by_day_of_month",
        timeOfDay: {
          hour: 0,
          minute: 0
        },
        dayOfMonth: 1,
        timeZone: "America/Los_Angeles"
      });

      const invalidMonthlyTime = newRecurrence({
        frequency: 5,
        typeName: "monthly_by_day_of_month",
        timeOfDay: {
          hour: 24,
          minute: 0
        },
        dayOfMonth: 15,
        timeZone: "America/Los_Angeles"
      });

      const invalidMonthlyDay = newRecurrence({
        frequency: 10,
        typeName: "monthly_by_day_of_month",
        timeOfDay: {
          hour: 23,
          minute: 59
        },
        dayOfMonth: 0,
        timeZone: "America/Los_Angeles"
      });

      const invalidMonthlyDay2 = newRecurrence({
        frequency: 10,
        typeName: "monthly_by_day_of_month",
        timeOfDay: {
          hour: 23,
          minute: 59
        },
        dayOfMonth: 32,
        timeZone: "America/Los_Angeles"
      });

      const invalidMonthlyTz = newRecurrence({
        frequency: 10,
        typeName: "monthly_by_day_of_month",
        timeOfDay: {
          hour: 23,
          minute: 59
        },
        dayOfMonth: 1,
        timeZone: ""
      });

      expect(invalidMonthlyFrequency.isValid()).toBe(false);
      expect(invalidMonthlyTime.isValid()).toBe(false);
      expect(invalidMonthlyDay.isValid()).toBe(false);
      expect(invalidMonthlyDay2.isValid()).toBe(false);
      expect(invalidMonthlyTz.isValid()).toBe(false);
    });
  });

  describe('monthly by nth day of week', () => {
    it('returns isValid true for valid monthly by nth day of week recurrences', () => {
      const monthly1 = newRecurrence({
        frequency: 1,
        typeName: "monthly_by_nth_day_of_week",
        timeOfDay: {
          hour: 0,
          minute: 0
        },
        nthDayOfWeek: 1,
        dayOfWeek: 1,
        timeZone: "America/Los_Angeles"
      });

      const monthly2 = newRecurrence({
        frequency: 5,
        typeName: "monthly_by_nth_day_of_week",
        timeOfDay: {
          hour: 12,
          minute: 30
        },
        nthDayOfWeek: 3,
        dayOfWeek: 4,
        timeZone: "America/Los_Angeles"
      });

      const monthly3 = newRecurrence({
        frequency: 10,
        typeName: "monthly_by_nth_day_of_week",
        timeOfDay: {
          hour: 23,
          minute: 59
        },
        nthDayOfWeek: 5,
        dayOfWeek: 7,
        timeZone: "America/Los_Angeles"
      });

      expect(monthly1.isValid()).toBe(true);
      expect(monthly2.isValid()).toBe(true);
      expect(monthly3.isValid()).toBe(true);
    });

    it('returns isValid false for invalid monthly by nth day of week recurrences', () => {
      const invalidMonthlyFrequency = newRecurrence({
        frequency: 0,
        typeName: "monthly_by_nth_day_of_week",
        timeOfDay: {
          hour: 0,
          minute: 0
        },
        nthDayOfWeek: 1,
        dayOfWeek: 1,
        timeZone: "America/Los_Angeles"
      });

      const invalidMonthlyTime = newRecurrence({
        frequency: 5,
        typeName: "monthly_by_nth_day_of_week",
        timeOfDay: {
          hour: 23,
          minute: 65
        },
        nthDayOfWeek: 3,
        dayOfWeek: 4,
        timeZone: "America/Los_Angeles"
      });

      const invalidMonthlyNthDay = newRecurrence({
        frequency: 10,
        typeName: "monthly_by_nth_day_of_week",
        timeOfDay: {
          hour: 23,
          minute: 59
        },
        nthDayOfWeek: 0,
        dayOfWeek: 7,
        timeZone: "America/Los_Angeles"
      });

      const invalidMonthlyNthDay2 = newRecurrence({
        frequency: 10,
        typeName: "monthly_by_nth_day_of_week",
        timeOfDay: {
          hour: 23,
          minute: 59
        },
        nthDayOfWeek: 6,
        dayOfWeek: 7,
        timeZone: "America/Los_Angeles"
      });

      const invalidMonthlyDayOfWeek = newRecurrence({
        frequency: 10,
        typeName: "monthly_by_nth_day_of_week",
        timeOfDay: {
          hour: 23,
          minute: 59
        },
        nthDayOfWeek: 1,
        dayOfWeek: 0,
        timeZone: "America/Los_Angeles"
      });

      const invalidMonthlyDayOfWeek2 = newRecurrence({
        frequency: 10,
        typeName: "monthly_by_nth_day_of_week",
        timeOfDay: {
          hour: 23,
          minute: 59
        },
        nthDayOfWeek: 1,
        dayOfWeek: 8,
        timeZone: "America/Los_Angeles"
      });

      const invalidMonthlyTz = newRecurrence({
        frequency: 10,
        typeName: "monthly_by_nth_day_of_week",
        timeOfDay: {
          hour: 23,
          minute: 59
        },
        nthDayOfWeek: 1,
        dayOfWeek: 7,
        timeZone: null
      });

      expect(invalidMonthlyFrequency.isValid()).toBe(false);
      expect(invalidMonthlyTime.isValid()).toBe(false);
      expect(invalidMonthlyNthDay.isValid()).toBe(false);
      expect(invalidMonthlyNthDay2.isValid()).toBe(false);
      expect(invalidMonthlyDayOfWeek.isValid()).toBe(false);
      expect(invalidMonthlyDayOfWeek2.isValid()).toBe(false);
      expect(invalidMonthlyTz.isValid()).toBe(false);
    });
  });

  describe('yearly', () => {
    it('returns isValid true for valid yearly recurrences', () => {
      const yearly1 = newRecurrence({
        frequency: 1,
        typeName: "yearly",
        timeOfDay: {
          hour: 0,
          minute: 0
        },
        dayOfMonth: 1,
        month: 1,
        timeZone: "America/Los_Angeles"
      });

      const yearly2 = newRecurrence({
        frequency: 5,
        typeName: "yearly",
        timeOfDay: {
          hour: 12,
          minute: 30
        },
        dayOfMonth: 15,
        month: 6,
        timeZone: "America/Los_Angeles"
      });

      const yearly3 = newRecurrence({
        frequency: 10,
        typeName: "yearly",
        timeOfDay: {
          hour: 23,
          minute: 59
        },
        dayOfMonth: 31,
        month: 12,
        timeZone: "America/Los_Angeles"
      });

      expect(yearly1.isValid()).toBe(true);
      expect(yearly2.isValid()).toBe(true);
      expect(yearly3.isValid()).toBe(true);
    });

    it('returns isValid false for invalid yearly recurrences', () => {
      const invalidYearlyFrequency = newRecurrence({
        frequency: 0,
        typeName: "yearly",
        timeOfDay: {
          hour: 0,
          minute: 0
        },
        dayOfMonth: 1,
        month: 1,
        timeZone: "America/Los_Angeles"
      });

      const invalidYearlyTime = newRecurrence({
        frequency: 5,
        typeName: "yearly",
        timeOfDay: {
          hour: 24,
          minute: 0
        },
        dayOfMonth: 15,
        month: 1,
        timeZone: "America/Los_Angeles"
      });

      const invalidYearlyDay1 = newRecurrence({
        frequency: 10,
        typeName: "yearly",
        timeOfDay: {
          hour: 23,
          minute: 59
        },
        dayOfMonth: 0,
        month: 1,
        timeZone: "America/Los_Angeles"
      });

      const invalidYearlyDay2 = newRecurrence({
        frequency: 10,
        typeName: "yearly",
        timeOfDay: {
          hour: 23,
          minute: 59
        },
        dayOfMonth: 32,
        month: 1,
        timeZone: "America/Los_Angeles"
      });

      const invalidYearlyMonth1 = newRecurrence({
        frequency: 10,
        typeName: "yearly",
        timeOfDay: {
          hour: 23,
          minute: 59
        },
        dayOfMonth: 31,
        month: 0,
        timeZone: "America/Los_Angeles"
      });

      const invalidYearlyMonth2 = newRecurrence({
        frequency: 10,
        typeName: "yearly",
        timeOfDay: {
          hour: 23,
          minute: 59
        },
        dayOfMonth: 31,
        month: 13,
        timeZone: "America/Los_Angeles"
      });

      const invalidYearlyTz = newRecurrence({
        frequency: 10,
        typeName: "yearly",
        timeOfDay: {
          hour: 23,
          minute: 59
        },
        dayOfMonth: 1,
        timeZone: ""
      });

      expect(invalidYearlyFrequency.isValid()).toBe(false);
      expect(invalidYearlyTime.isValid()).toBe(false);
      expect(invalidYearlyDay1.isValid()).toBe(false);
      expect(invalidYearlyDay2.isValid()).toBe(false);
      expect(invalidYearlyMonth1.isValid()).toBe(false);
      expect(invalidYearlyMonth2.isValid()).toBe(false);
      expect(invalidYearlyTz.isValid()).toBe(false);
    });
  });
});

