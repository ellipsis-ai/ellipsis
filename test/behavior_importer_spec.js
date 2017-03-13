import React from 'react';
import TestUtils from 'react-addons-test-utils';
const BehaviorImporter = require('../app/assets/javascripts/behavior_importer/index');
const BehaviorGroup = require('../app/assets/javascripts/models/behavior_group');
const BehaviorGroupCard = require('../app/assets/javascripts/behavior_list/behavior_group_card');

describe('BehaviorImporter', () => {
  const group1 = Object.freeze(BehaviorGroup.fromJson({
    behaviorVersions: [
      {
        exportId: "1",
        functionBody: "onSuccess('Woot')",
        responseTemplate: "{successResult}",
        params: [],
        triggers: [{
          text: "Do the tests run?",
          requiresMention: false,
          isRegex: false,
          caseSensitive: false
        }],
        config: {},
        knownEnvVarsUsed: [],
        shouldRevealCodeEditor: true
      }
    ],
    description: "Make it so",
    exportId: "10000",
    githubUrl: "https://not.a.real.domain.name/",
    icon: "*",
    name: "Number 1",
    teamId: "54321"
  }));

  const group2 = Object.freeze(BehaviorGroup.fromJson({
    behaviorVersions: [
      {
        exportId: "2",
        functionBody: "onSuccess('Woot')",
        responseTemplate: "{successResult}",
        params: [],
        triggers: [{
          text: "how do the tests run",
          requiresMention: false,
          isRegex: false,
          caseSensitive: false
        }],
        config: {},
        knownEnvVarsUsed: [],
        shouldRevealCodeEditor: true
      }
    ],
    description: "Earl Grey, Hot",
    exportId: "10001",
    githubUrl: "https://not.a.real.domain.name/",
    icon: "*",
    name: "Tea",
    teamId: "54321"
  }));

  const defaultConfig = Object.freeze({
    csrfToken: "2",
    behaviorGroups: [group1, group2],
    installedBehaviorGroups: [],
    teamId: "54321"
  });

  function createBehaviorImporter(config) {
    return TestUtils.renderIntoDocument(
      <BehaviorImporter {...config} />
    ).refs.component;
  }

  let config = {};

  beforeEach(() => {
    config = Object.assign(config, defaultConfig);
  });

  describe('render', () => {
    it('renders a card for each group', () => {
      const list = createBehaviorImporter(config);
      expect(TestUtils.scryRenderedComponentsWithType(list, BehaviorGroupCard).length).toEqual(config.behaviorGroups.length);
    });
  });

});
