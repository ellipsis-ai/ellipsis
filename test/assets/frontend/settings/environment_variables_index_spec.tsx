import * as React from 'react';
import * as TestUtils from 'react-dom/test-utils';
import EnvironmentVariables from '../../../../app/assets/frontend/settings/environment_variables/index';
import {EnvironmentVariableListConfig} from "../../../../app/assets/frontend/settings/environment_variables/loader";
import {getPageRequiredProps} from "../../../mocks/mock_page";

jsRoutes.controllers.APITokenController.listTokens = () => ({ url: '/mock_list_tokens', method: "get", absoluteURL: () => "" });
jsRoutes.controllers.GithubConfigController.index = () => ({ url: '/mock_github_config', method: "get", absoluteURL: () => "" });
jsRoutes.controllers.web.settings.EnvironmentVariablesController.list = () => ({ url: '/mock_environment_variables_list', method: "get", absoluteURL: () => "" });
jsRoutes.controllers.web.settings.RegionalSettingsController.index = () => ({ url: '/mock_regional_settings', method: "get", absoluteURL: () => "" });
jsRoutes.controllers.web.settings.IntegrationsController.list = () => ({ url: '/mock_integrations', method: "get", absoluteURL: () => "" });

describe('EnvironmentVariables', () => {

  const defaultConfig: EnvironmentVariableListConfig = {
    csrfToken: "0",
    containerId: "foo",
    isAdmin: false,
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
  };

  function createIndex(config: EnvironmentVariableListConfig): EnvironmentVariables {
    return TestUtils.renderIntoDocument(
      <EnvironmentVariables {...config} {...getPageRequiredProps()} />
    ) as EnvironmentVariables;
  }

  let config: EnvironmentVariableListConfig;

  beforeEach(() => {
    config = Object.assign({}, defaultConfig);
  });

  describe('groupAndSortVarsByNameAndPresenceOfValue', () => {
    it('puts variables with values before those without, and sorts each alphabetically', () => {
      const index = createIndex(config);
      const vars = index.groupAndSortVarsByNameAndPresenceOfValue(index.props.data.variables);
      expect(vars.map((ea) => ea.name)).toEqual(["ONE", "THREE", "FOUR", "TWO"]);
    });
  });

});
