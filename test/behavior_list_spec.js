jest.unmock('../app/assets/javascripts/behavior_list/index');

import React from 'react';
import ReactDOM from 'react-dom';
import TestUtils from 'react-addons-test-utils';
const BehaviorList = require('../app/assets/javascripts/behavior_list/index');

describe('BehaviorList', () => {
  jsRoutes.controllers.ApplicationController.editBehavior = function() { return '/edit'; };

  const defaultConfig = {
    behaviorVersions: [{
      "teamId": "abcdef",
      "behaviorId": "ghijkl",
      "functionBody": "use strict;",
      "responseTemplate": "A template",
      "params": [],
      "triggers": [{
        "text": "aws certificates",
        "requiresMention": false,
        "isRegex": false,
        "caseSensitive": false
      }, {
        "text": "aws certs",
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
    }, {
      "teamId": "abcdef",
      "behaviorId": "mnopqr",
      "functionBody": "use strict;",
      "responseTemplate": "A template",
      "params": [],
      "triggers": [{
        "text": "aws servers",
        "requiresMention": true,
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
      "createdAt": 1468359271138
    }, {
      "teamId": "abcdef",
      "behaviorId": "stuvwx",
      "functionBody": "",
      "responseTemplate": "The magic 8-ball says:\n\n“Concentrate and ask aga sdfsadfsdin”",
      "params": [],
      "triggers": [],
      "config": {},
      "createdAt": 1466109904858
    }]
  };

  function createBehaviorList(config) {
    return TestUtils.renderIntoDocument(
      <BehaviorList {...config} />
    );
  }

  let config = {};

  beforeEach(() => {
    config = Object.assign(config, defaultConfig);
  });

  describe('render', () => {
    it('renders a table', () => {
      const list = createBehaviorList(config);
      const output = list.render();
      expect(output.type).toBe('table');
    });
  });
});
