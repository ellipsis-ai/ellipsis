jest.unmock('../app/assets/javascripts/behavior_list/index');
jest.unmock('../app/assets/javascripts/sort');
jest.unmock('../app/assets/javascripts/models/behavior_version');
jest.unmock('../app/assets/javascripts/models/param');
jest.unmock('../app/assets/javascripts/models/response_template');
jest.unmock('../app/assets/javascripts/models/trigger');

import React from 'react';
import TestUtils from 'react-addons-test-utils';
const BehaviorList = require('../app/assets/javascripts/behavior_list/index');
const BehaviorVersion = require('../app/assets/javascripts/models/behavior_version');

describe('BehaviorList', () => {
  jsRoutes.controllers.BehaviorEditorController.edit = function() { return '/edit'; };

  const behaviorVersionTask1 = Object.freeze({
    "teamId": "abcdef",
    "groupId": "sfgsdf",
    "behaviorId": "ghijkl",
    "functionBody": "use strict;",
    "responseTemplate": "A template",
    "params": [],
    "triggers": [{
      "text": "B",
      "requiresMention": false,
      "isRegex": true,
      "caseSensitive": false
    }, {
      "text": "C",
      "requiresMention": false,
      "isRegex": false,
      "caseSensitive": false
    }],
    "config": {
      "aws": {
        "accessKeyName": "AWS_ACCESS_KEY",
        "secretKeyName": "AWS_SECRET_KEY",
        "regionName": "AWS_REGION"
      }
    },
    "createdAt": 1468338136532
  });
  const behaviorVersionTask2 = Object.freeze({
    "teamId": "abcdef",
    "groupId": "gsdfgsg",
    "behaviorId": "mnopqr",
    "functionBody": "use strict;",
    "responseTemplate": "A template",
    "params": [],
    "triggers": [{
      "text": "A",
      "requiresMention": true,
      "isRegex": true,
      "caseSensitive": false
    }],
    "config": {
      "aws": {
        "accessKeyName": "AWS_ACCESS_KEY",
        "secretKeyName": "AWS_SECRET_KEY",
        "regionName": "AWS_REGION"
      }
    },
    "createdAt": 1468359271138
  });
  const behaviorVersionKnowledge1 = Object.freeze({
    "teamId": "abcdef",
    "groupId": "jfghjfg",
    "behaviorId": "stuvwx",
    "functionBody": "",
    "responseTemplate": "The magic 8-ball says:\n\n“Concentrate and ask again.”",
    "params": [],
    "triggers": [],
    "config": {},
    "createdAt": 1466109904858
  });
  const defaultConfig = Object.freeze({
    behaviorGroups: [
      {"id":"sfgsdf", "name":"", "createdAt": 1466109904858},
      {"id":"gsdfgsg", "name":"", "createdAt": 1466109904858},
      {"id":"jfghjfg", "name":"", "createdAt": 1466109904858}
    ],
    behaviorVersions: [behaviorVersionTask1, behaviorVersionTask2, behaviorVersionKnowledge1].map((ea) => BehaviorVersion.fromJson(ea))
  });

  function createBehaviorList(config) {
    return TestUtils.renderIntoDocument(
      <BehaviorList {...config} />
    );
  }

  let config = {};

  beforeEach(() => {
    config = Object.assign(config, defaultConfig);
  });

  describe('getTableRowClasses', () => {
    it('adds a border and top padding to every 3rd index', () => {
      const list = createBehaviorList(config);
      const classes0 = list.getTableRowClasses(0);
      const classes3 = list.getTableRowClasses(3);
      const classes6 = list.getTableRowClasses(6);
      expect(classes0).toContain(' pt');
      expect(classes0).toContain(' border-top ');
      expect(classes3).toContain(' pt');
      expect(classes3).toContain(' border-top ');
      expect(classes6).toContain(' pt');
      expect(classes6).toContain(' border-top ');
    });

    it('adds bottom padding to every 3rd index minus one', () => {
      const list = createBehaviorList(config);
      const classes2 = list.getTableRowClasses(2);
      const classes5 = list.getTableRowClasses(5);
      const classes8 = list.getTableRowClasses(8);
      expect(classes2).toContain(' pb');
      expect(classes5).toContain(' pb');
      expect(classes8).toContain(' pb');
    });

    it('adds nothing to every 3rd index minus two', () => {
      const list = createBehaviorList(config);
      const classes1 = list.getTableRowClasses(1);
      const classes4 = list.getTableRowClasses(4);
      const classes7 = list.getTableRowClasses(7);
      expect(classes1).toBe('');
      expect(classes4).toBe('');
      expect(classes7).toBe('');
    });
  });
});
