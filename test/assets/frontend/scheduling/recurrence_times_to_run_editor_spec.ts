import * as React from 'react';
import * as TestUtils from "react-addons-test-utils";
import RecurrenceTimesToRunEditor from "../../../../app/assets/frontend/scheduling/recurrence_times_to_run_editor";
import Recurrence, {RecurrenceInterface} from "../../../../app/assets/frontend/models/recurrence";
import clearAllMocks = jest.clearAllMocks;

const onChangeSpy = jest.fn();

function createEditorWithRecurrence(recurrenceProps?: Partial<RecurrenceInterface>): RecurrenceTimesToRunEditor {
  return TestUtils.renderIntoDocument(React.createElement(RecurrenceTimesToRunEditor, {
    recurrence: new Recurrence({}).becomeMinutely().clone(recurrenceProps || {}),
    onChange: onChangeSpy
  })) as RecurrenceTimesToRunEditor;
}

describe("RecurrenceTimesToRunEditor", () => {
  describe("toggleTimesToRun", () => {
    it("sets the times to run to null if disabled", () => {
      const editor = createEditorWithRecurrence({
        totalTimesToRun: 5
      });
      const spy = spyOn(editor, "setTotalTimesToRun");
      editor.toggleTimesToRun(false);
      expect(spy).toBeCalledWith(null);
    });

    it("sets the times to run to 1 if it was null if enabled", () => {
      const editor = createEditorWithRecurrence({
        totalTimesToRun: null
      });
      const spy = spyOn(editor, "setTotalTimesToRun");
      editor.toggleTimesToRun(true);
      expect(spy).toBeCalledWith(1);
    });

    it("maintains the times to run if it existed and it's enabled", () => {
      const editor = createEditorWithRecurrence({
        totalTimesToRun: 5
      });
      const spy = spyOn(editor, "setTotalTimesToRun");
      editor.toggleTimesToRun(true);
      expect(spy).toBeCalledWith(5);
    });
  });

  describe("setTotalTimesToRun", () => {
    beforeEach(() => {
      clearAllMocks();
    });

    it("resets frequency to 1 if totalTimesToRun is set to 1", () => {
      const editor = createEditorWithRecurrence({
        frequency: 5,
        daysOfWeek: [],
        totalTimesToRun: null,
        timesHasRun: 0
      });
      editor.setTotalTimesToRun(1);
      expect(onChangeSpy.mock.calls[0][0]).toMatchObject({
        frequency: 1,
        daysOfWeek: [],
        totalTimesToRun: 1,
        timesHasRun: 0
      });
    });

    it("leaves frequency as is if totalTimesToRun is set to null or > 1", () => {
      const editor = createEditorWithRecurrence({
        frequency: 5,
        daysOfWeek: [],
        totalTimesToRun: null,
        timesHasRun: 0
      });
      editor.setTotalTimesToRun(2);
      editor.setTotalTimesToRun(5);
      editor.setTotalTimesToRun(null);
      expect(onChangeSpy.mock.calls[0][0]).toMatchObject({
        frequency: 5,
        daysOfWeek: [],
        totalTimesToRun: 2,
        timesHasRun: 0
      });
      expect(onChangeSpy.mock.calls[1][0]).toMatchObject({
        frequency: 5,
        daysOfWeek: [],
        totalTimesToRun: 5,
        timesHasRun: 0
      });
      expect(onChangeSpy.mock.calls[2][0]).toMatchObject({
        frequency: 5,
        daysOfWeek: [],
        totalTimesToRun: null,
        timesHasRun: 0
      });
    });

    it("sets times has run to 0 if totalTimesToRun is changed from null", () => {
      const editor = createEditorWithRecurrence({
        frequency: 5,
        daysOfWeek: [],
        totalTimesToRun: null,
        timesHasRun: 5
      });
      editor.setTotalTimesToRun(2);
      expect(onChangeSpy.mock.calls[0][0]).toMatchObject({
        frequency: 5,
        daysOfWeek: [],
        totalTimesToRun: 2,
        timesHasRun: 0
      });
    });

    it("sets times has run to 0 if totalTimesToRun is changed from one number to another", () => {
      const editor = createEditorWithRecurrence({
        frequency: 5,
        daysOfWeek: [],
        totalTimesToRun: 5,
        timesHasRun: 4
      });
      editor.setTotalTimesToRun(2);
      expect(onChangeSpy.mock.calls[0][0]).toMatchObject({
        frequency: 5,
        daysOfWeek: [],
        totalTimesToRun: 2,
        timesHasRun: 0
      });
    });

    it("leaves times has run as is if totalTimesToRun is unchanged", () => {
      const editor = createEditorWithRecurrence({
        frequency: 5,
        daysOfWeek: [],
        totalTimesToRun: 5,
        timesHasRun: 4
      });
      editor.setTotalTimesToRun(5);
      expect(onChangeSpy.mock.calls[0][0]).toMatchObject({
        frequency: 5,
        daysOfWeek: [],
        totalTimesToRun: 5,
        timesHasRun: 4
      });
    });

    it("slices days of the week from the start to ensure its length doesn't exceed the times to run", () => {
      const editor = createEditorWithRecurrence({
        frequency: 1,
        daysOfWeek: [1, 2, 3],
        totalTimesToRun: 3,
        timesHasRun: 0
      });
      editor.setTotalTimesToRun(2);
      expect(onChangeSpy.mock.calls[0][0]).toMatchObject({
        frequency: 1,
        daysOfWeek: [2, 3],
        totalTimesToRun: 2,
        timesHasRun: 0
      });

      const editor2 = createEditorWithRecurrence({
        frequency: 1,
        daysOfWeek: [1, 2, 3],
        totalTimesToRun: 3,
        timesHasRun: 0
      });
      editor2.setTotalTimesToRun(1);
      expect(onChangeSpy.mock.calls[1][0]).toMatchObject({
        frequency: 1,
        daysOfWeek: [3],
        totalTimesToRun: 1,
        timesHasRun: 0
      });

      const editor3 = createEditorWithRecurrence({
        frequency: 1,
        daysOfWeek: [1, 2, 3],
        totalTimesToRun: 5,
        timesHasRun: 0
      });
      editor3.setTotalTimesToRun(3);
      expect(onChangeSpy.mock.calls[2][0]).toMatchObject({
        frequency: 1,
        daysOfWeek: [1, 2, 3],
        totalTimesToRun: 3,
        timesHasRun: 0
      });
    });
  });
});
