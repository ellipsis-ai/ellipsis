import * as React from 'react';
import * as TestUtils from "react-addons-test-utils";
import {RecurrenceInterface} from "../../../../app/assets/frontend/models/recurrence";
import Recurrence from "../../../../app/assets/frontend/models/recurrence";
import TimeOfDayEditor from "../../../../app/assets/frontend/scheduling/time_of_day_editor";

const onChangeSpy = jest.fn();

function createEditorWithRecurrence(recurrenceProps?: Partial<RecurrenceInterface>): TimeOfDayEditor {
  return TestUtils.renderIntoDocument((
    <TimeOfDayEditor
      recurrence={new Recurrence({}).becomeDaily().clone(recurrenceProps || {})}
      onChange={onChangeSpy}
      teamTimeZone={"America/Toronto"}
      teamTimeZoneName={"Eastern time"}
    />
  )) as TimeOfDayEditor;
}

describe("TimeOfDayEditor", () => {
  describe("onChangeHour", () => {
    it("ensures >= 12 is a valid AM hour when already in AM", () => {
      const editor = createEditorWithRecurrence({
        timeOfDay: {
          hour: 1,
          minute: 0
        }
      });
      jest.resetAllMocks();
      editor.onChangeHour("0");
      editor.onChangeHour("5");
      editor.onChangeHour("12");
      const calls = onChangeSpy.mock.calls;
      expect(calls[0][0]).toMatchObject({
        timeOfDay: {
          hour: 0,
          minute: 0
        }
      });
      expect(calls[1][0]).toMatchObject({
        timeOfDay: {
          hour: 5,
          minute: 0
        }
      });
      expect(calls[2][0]).toMatchObject({
        timeOfDay: {
          hour: 0,
          minute: 0
        }
      });
    });

    it("converts < 12 to PM when already in PM", () => {
      const editor = createEditorWithRecurrence({
        timeOfDay: {
          hour: 13,
          minute: 0
        }
      });
      jest.resetAllMocks();
      editor.onChangeHour("12");
      editor.onChangeHour("5");
      editor.onChangeHour("1");
      const calls = onChangeSpy.mock.calls;
      expect(calls[0][0]).toMatchObject({
        timeOfDay: {
          hour: 12,
          minute: 0
        }
      });
      expect(calls[1][0]).toMatchObject({
        timeOfDay: {
          hour: 17,
          minute: 0
        }
      });
      expect(calls[2][0]).toMatchObject({
        timeOfDay: {
          hour: 13,
          minute: 0
        }
      });
    });

    it("does nothing with invalid hours", () => {
      const editor = createEditorWithRecurrence({
        timeOfDay: {
          hour: 0,
          minute: 0
        }
      });
      jest.resetAllMocks();
      editor.onChangeHour("A");
      expect(onChangeSpy).not.toBeCalled();
    });
  })
});
