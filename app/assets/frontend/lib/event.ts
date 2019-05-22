import * as React from 'react';
import {KeyboardShortcutInterface} from "./keyboard_shortcut";

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

    static keyPressIncludedPlatformCommandKey(event: AnyKeyboardEvent, shiftRequired?: boolean): boolean {
      const eventIncludedShift = Event.keyPressIncludedShift(event);
      const correctShiftPosition = shiftRequired ? eventIncludedShift : !eventIncludedShift;
      if (/^Mac/.test(navigator.platform)) {
        return event.metaKey && !event.altKey && correctShiftPosition && !event.ctrlKey;
      } else if (/^Win/.test(navigator.platform)) {
        return event.ctrlKey && !event.altKey && correctShiftPosition && !event.metaKey;
      } else {
        // On non Windows/Mac platforms, don't do anything rash
        return false;
      }
    }

    static keyPressMatchesShortcut(event: AnyKeyboardEvent, shortcut: KeyboardShortcutInterface): boolean {
      if (shortcut.command) {
        return Event.keyPressIncludedPlatformCommandKey(event, shortcut.shift) && event.key === shortcut.key;
      } else {
        return Event.keyPressIncludedShift(event) && event.key === shortcut.key;
      }
    }
}

export default Event;
