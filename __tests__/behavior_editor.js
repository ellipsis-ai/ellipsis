jest.unmock('../app/assets/javascripts/behavior_editor');

import React from 'react';
import ReactDOM from 'react-dom';
import TestUtils from 'react-addons-test-utils';
var BehaviorEditor = require('../app/assets/javascripts/behavior_editor');

describe('BehaviorEditor', () => {
  const config = {
    teamId: "A",
    behaviorId: "1",
    nodeFunction: "onSuccess('Woot')",
    responseTemplate: "{successResult}",
    params: [],
    triggers: [{
      text: "Do the tests run?",
      requiresMention: false,
      isRegex: false,
      caseSensitive: false
    }],
    csrfToken: "2",
    justSaved: false,
    envVariableNames: ["HOT_DOG"],
    shouldRevealCodeEditor: true
  };

  const editor = TestUtils.renderIntoDocument(
    <BehaviorEditor {...config} />
  );

  describe('utils', () => {
    describe('arrayWithNewElementAtIndex', () => {
      const array = [0, 1, 2];

      it('copies an array before modifying it', () => {
        const newArray = editor.utils.arrayWithNewElementAtIndex(array, 3, 2);
        expect(array).toEqual([0, 1, 2]);
        expect(newArray).toEqual([0, 1, 3]);
      });
    });
  });
});
