jest.unmock('../app/assets/javascripts/behavior_editor');

import React from 'react';
import ReactDOM from 'react-dom';
import TestUtils from 'react-addons-test-utils';
var BehaviorEditor = require('../app/assets/javascripts/behavior_editor');

describe('BehaviorEditor', () => {
  const BehaviorEditorConfiguration = {
    data: {"teamId":"IRnpTuI4SBmbX1Xxuns6tg","behaviorId":"hMIiD-LlQuegerpGpa1zTg","nodeFunction":"\"use strict\";\nconst moment = require('moment-timezone');\nconst t = moment();\nlet tz;\nlet result = '';\n\nif (place.match(/^(san fran|los angeles|vancouver|portland|seattle)/i)) {\n  tz = 'America/Los_Angeles';\n} else if (place.match(/^(toronto|montreal|ottawa|new york|boston|philadelphia|washington|atlanta|miami)/i)) {\n  tz = 'America/New_York';\n} else {\n  tz = 'UTC';\n  result =\n    `I only know the time of day for a few places and you said “${place}”. \n    Try Los Angeles, San Francisco, or Vancouver.\\n\\n\\n`;\n}\n\nresult += `It’s ${t.tz(tz).format('LT')} `;\nresult += (tz == 'UTC' ? 'UTC' : `in ${place}`) + '.';\nonSuccess(result);","responseTemplate":"{successResult}","params":[{"name":"place","question":"For what place do you want to know the time?"}],"triggers":[{"text":"What time is it in another city?","requiresMention":false,"isRegex":false,"caseSensitive":false},{"text":"what time is it in ([^?]+)\\?*\\s*","requiresMention":false,"isRegex":true,"caseSensitive":false}]},
    containerId: 'editorContainer',
    csrfToken: "e80e143780da848a397801480ede54cc5aeba70a-1465938166515-05f6b78055085a43686fe648",
    justSaved: false,
    envVariableNames: ["HOT_DOG"]
  };

  const editor = TestUtils.renderIntoDocument(
    <BehaviorEditor teamId="0" csrfToken="1" />
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
