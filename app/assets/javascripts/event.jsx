define(function() {
  class Event {
    static keyPressWasDown(event) {
      return event.which === 40;
    }

    static keyPressWasEnter(event) {
      return event.which === 13;
    }

    static keyPressWasEsc(event) {
      return event.which === 27;
    }

    static keyPressWasLeft(event) {
      return event.which === 37;
    }

    static keyPressWasRight(event) {
      return event.which === 39;
    }

    static keyPressWasSpace(event) {
      return event.which === 32;
    }

    static keyPressWasTab(event) {
      return event.which === 9;
    }

    static keyPressWasUp(event) {
      return event.which === 38;
    }

    static keyPressIncludedShift(event) {
      return event.shiftKey;
    }
  }

  return Event;
});
