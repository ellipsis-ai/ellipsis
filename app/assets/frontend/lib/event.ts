import * as React from 'react';

export type AnyKeyboardEvent = KeyboardEvent | React.KeyboardEvent<any>

class Event {
    static keyPressWasDown(event: AnyKeyboardEvent): boolean {
      return event.which === 40;
    }

    static keyPressWasEnter(event: AnyKeyboardEvent): boolean {
      return event.which === 13;
    }

    static keyPressWasEsc(event: AnyKeyboardEvent): boolean {
      return event.which === 27;
    }

    static keyPressWasLeft(event: AnyKeyboardEvent): boolean {
      return event.which === 37;
    }

    static keyPressWasRight(event: AnyKeyboardEvent): boolean {
      return event.which === 39;
    }

    static keyPressWasSpace(event: AnyKeyboardEvent): boolean {
      return event.which === 32;
    }

    static keyPressWasTab(event: AnyKeyboardEvent): boolean {
      return event.which === 9;
    }

    static keyPressWasUp(event: AnyKeyboardEvent): boolean {
      return event.which === 38;
    }

    static keyPressIncludedShift(event: AnyKeyboardEvent): boolean {
      return event.shiftKey;
    }

    static keyPressWasSaveShortcut(event: AnyKeyboardEvent): boolean {
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
