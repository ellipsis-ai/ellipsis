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

    static keyPressWasSaveShortcut(event) {
      var sKeyWhich = 83;
      if (/^Mac/.test(navigator.platform)) {
        return event.metaKey && !event.altKey && !event.shiftKey && !event.ctrlKey && event.which === sKeyWhich;
      } else if (/^Win/.test(navigator.platform)) {
        return event.ctrlKey && !event.altKey && !event.shiftKey && !event.metaKey && event.which === sKeyWhich;
      } else {
        // On non Windows/Mac platforms, don't do anything rash
        return false;
      }
    }
  }

  return Event;
});
