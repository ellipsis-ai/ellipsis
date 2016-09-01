jest.unmock('../app/assets/javascripts/behavior_list/index');

import React from 'react';
import TestUtils from 'react-addons-test-utils';
const BehaviorList = require('../app/assets/javascripts/behavior_list/index');

describe('BehaviorList', () => {
  jsRoutes.controllers.BehaviorEditorController.edit = function() { return '/edit'; };

  const behaviorVersionTask1 = Object.freeze({
    "teamId": "abcdef",
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
    "behaviorId": "stuvwx",
    "functionBody": "",
    "responseTemplate": "The magic 8-ball says:\n\n“Concentrate and ask again.”",
    "params": [],
    "triggers": [],
    "config": {},
    "createdAt": 1466109904858
  });
  const defaultConfig = Object.freeze({
    behaviorVersions: [behaviorVersionTask1, behaviorVersionTask2, behaviorVersionKnowledge1]
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

  describe('getDisplayTriggerFromVersion', () => {
    it('returns the first non-regex trigger when available', () => {
      const list = createBehaviorList(config);
      const result = list.getDisplayTriggerFromVersion(behaviorVersionTask1);
      expect(result.index).toBe(1);
    });

    it('returns an empty string when there’s no first trigger', () => {
      const list = createBehaviorList(config);
      const result = list.getDisplayTriggerFromVersion(behaviorVersionKnowledge1);
      expect(result.text).toBe("");
    });

    it('returns the first trigger when they’re all regex', () => {
      const list = createBehaviorList(config);
      const result = list.getDisplayTriggerFromVersion(behaviorVersionTask2);
      expect(result.index).toBe(0);
    });
  });

  describe('getGroupedVersions', () => {
    it('groups the versions by type and sorts them alphabetically', () => {
      const list = createBehaviorList(config);
      const groups = list.getGroupedVersions();
      expect(groups).toEqual({
        tasks: [behaviorVersionTask2, behaviorVersionTask1],
        knowledge: [behaviorVersionKnowledge1]
      });
    });
  });

  describe('sortVersionsByFirstTrigger', () => {
    it('sorts alphabetically by first trigger, ignoring case', () => {
      const t1 = Object.assign({}, behaviorVersionTask1);
      t1.triggers[0].text = 'Be';
      const t2 = Object.assign({}, behaviorVersionTask1);
      t2.triggers[0].text = 'á winner';
      const t3 = Object.assign({}, behaviorVersionTask1);
      t3.triggers[0].text = 'baby';
      const t4 = Object.assign({}, behaviorVersionTask1);
      t4.triggers = [];
      const list = createBehaviorList(config);
      const sorted = list.sortVersionsByFirstTrigger([t1, t2, t3, t4]);
      expect(sorted).toEqual([t4, t2, t3, t1]);
    });
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
