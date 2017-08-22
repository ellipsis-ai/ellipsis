import React from 'react';
import TestUtils from 'react-addons-test-utils';

const EnvironmentVariables = require('../app/assets/javascripts/environment_variables/index');

jsRoutes.controllers.EnvironmentVariablesController.list = jest.fn(() => ({ url: '/mock_environment_variables_list' }));
jsRoutes.controllers.APITokenController.listTokens = jest.fn(() => ({ url: '/mock_list_tokens' }));
jsRoutes.controllers.OAuth2ApplicationController.list = jest.fn(() => ({ url: 'mock_oauth2_list' }));
jsRoutes.controllers.AWSConfigController.list = jest.fn(() => ({ url: 'mock_aws_config_list' }));


describe('EnvironmentVariables', () => {

  const defaultConfig = Object.freeze({
    csrfToken: "0",
    data: {
      teamId: "1",
      variables: [{
        name: "ONE",
        isAlreadySavedWithValue: true
      }, {
        name: "TWO",
        isAlreadySavedWithValue: false
      }, {
        name: "THREE",
        isAlreadySavedWithValue: true
      }, {
        name: "FOUR",
        isAlreadySavedWithValue: false
      }]
    }
  });

  function createIndex(config) {
    return TestUtils.renderIntoDocument(
      <EnvironmentVariables {...config} />
    );
  }

  let config = {};

  beforeEach(() => {
    config = Object.assign(config, defaultConfig);
  });

  describe('groupAndSortVarsByNameAndPresenceOfValue', () => {
    it('puts variables with values before those without, and sorts each alphabetically', () => {
      let index = createIndex(config);
      let vars = index.groupAndSortVarsByNameAndPresenceOfValue(index.props.data.variables);
      expect(vars.map((ea) => ea.name)).toEqual(["ONE", "THREE", "FOUR", "TWO"]);
    });
  });

});
