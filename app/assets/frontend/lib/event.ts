class Event {
    static keyPressWasDown(event: KeyboardEvent): boolean {
      return event.which === 40;
    }

    static keyPressWasEnter(event: KeyboardEvent): boolean {
      return event.which === 13;
    }

    static keyPressWasEsc(event: KeyboardEvent): boolean {
      return event.which === 27;
    }

    static keyPressWasLeft(event: KeyboardEvent): boolean {
      return event.which === 37;
    }

    static keyPressWasRight(event: KeyboardEvent): boolean {
      return event.which === 39;
    }

    static keyPressWasSpace(event: KeyboardEvent): boolean {
      return event.which === 32;
    }

    static keyPressWasTab(event: KeyboardEvent): boolean {
      return event.which === 9;
    }

    static keyPressWasUp(event: KeyboardEvent): boolean {
      return event.which === 38;
    }

    static keyPressIncludedShift(event: KeyboardEvent): boolean {
      return event.shiftKey;
    }

    static keyPressWasSaveShortcut(event: KeyboardEvent): boolean {
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

export default Event;
