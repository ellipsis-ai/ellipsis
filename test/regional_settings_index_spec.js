import React from 'react';
import TestUtils from 'react-addons-test-utils';
global.fetch = require('./mocks/mock_fetch');

const Page = require('../app/assets/javascripts/shared_ui/page');
const RegionalSettings = require('../app/assets/javascripts/regional_settings/index');
const TeamTimeZoneSetter = require('../app/assets/javascripts/time_zone/team_time_zone_setter');

jsRoutes.controllers.EnvironmentVariablesController.list = jest.fn(() => ({ url: '/mock_environment_variables_list' }));
jsRoutes.controllers.APITokenController.listTokens = jest.fn(() => ({ url: '/mock_list_tokens' }));
jsRoutes.controllers.OAuth2ApplicationController.list = jest.fn(() => ({ url: '/mock_oauth2_list' }));
jsRoutes.controllers.AWSConfigController.list = jest.fn(() => ({ url: '/mock_aws_config_list' }));
jsRoutes.controllers.RegionalSettingsController.index = jest.fn(() => ({ url: '/mock_regional_settings' }));
jsRoutes.controllers.ApplicationController.possibleCitiesFor = jest.fn(() => ({ url: '/mock_possible_cities' }));

describe('RegionalSettings', () => {
  const onSaveTimeZone = jest.fn();

  const defaultConfig = Object.freeze({
    csrfToken: "0",
    teamId: "1",
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