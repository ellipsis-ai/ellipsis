import * as React from 'react';
import * as TestUtils from 'react-addons-test-utils';
global.fetch = require('./../../../mocks/mock_fetch');

import Page from '../../../../app/assets/frontend/shared_ui/page';
import RegionalSettings from '../../../../app/assets/frontend/settings/regional_settings/index';

jsRoutes.controllers.APITokenController.listTokens = jest.fn(() => ({ url: '/mock_list_tokens' }));
jsRoutes.controllers.ApplicationController.possibleCitiesFor = jest.fn(() => ({ url: '/mock_possible_cities' }));
jsRoutes.controllers.GithubConfigController.index = jest.fn(() => ({ url: '/mock_github_config' }));
jsRoutes.controllers.web.settings.EnvironmentVariablesController.list = jest.fn(() => ({ url: '/mock_environment_variables_list' }));
jsRoutes.controllers.web.settings.RegionalSettingsController.index = jest.fn(() => ({ url: '/mock_regional_settings' }));
jsRoutes.controllers.web.settings.IntegrationsController.list = jest.fn(() => ({ url: '/mock_integrations' }));
jsRoutes.controllers.web.settings.OAuth2ApplicationController.list = jest.fn(() => ({ url: '/mock_oauth2_list' }));
jsRoutes.controllers.web.settings.AWSConfigController.list = jest.fn(() => ({ url: '/mock_aws_config_list' }));

describe('RegionalSettings', () => {
  const onSaveTimeZone = jest.fn();

  const defaultConfig = Object.freeze({
    csrfToken: "0",
    teamId: "1",
    isAdmin: false,
    onSaveTimeZone: onSaveTimeZone,
    teamTimeZone: "America/New_York",
    teamTimeZoneName: "Eastern Time",
    teamTimeZoneOffset: -14400
  });

  function createIndex(config) {
    return TestUtils.renderIntoDocument(
      <Page csrfToken={config.csrfToken} feedbackContainer={document.createElement('span')}>
        <RegionalSettings {...config} />
      </Page>
    ).component;
  }

  let config = {};

  beforeEach(() => {
    config = Object.assign({}, defaultConfig);
  });

  describe('render', () => {
    it('renders a team time zone setter when the time zone is set', () => {
      const index = createIndex(config);
      const spy = jest.spyOn(index, 'renderSetterPanel');
      index.render();
      expect(spy).toHaveBeenCalled();
    });

    it('renders a team time zone setter when there is no time zone', () => {
      const index = createIndex(Object.assign(config, {
        teamTimeZone: null,
        teamTimeZoneName: null,
        teamTimeZoneOffset: null
      }));
      const spy = jest.spyOn(index, 'renderSetterPanel');
      index.render();
      expect(spy).toHaveBeenCalled();
    });
  });

});