import * as React from 'react';
import * as TestUtils from 'react-dom/test-utils';
import mockFetch from './../../../mocks/mock_fetch';
global.fetch = mockFetch;

import Page from '../../../../app/assets/frontend/shared_ui/page';
import RegionalSettings, {RegionalSettingsProps} from '../../../../app/assets/frontend/settings/regional_settings/index';

jsRoutes.controllers.APITokenController.listTokens = () => ({ url: '/mock_list_tokens', method: "get", absoluteURL: () => "" });
jsRoutes.controllers.ApplicationController.possibleCitiesFor = () => ({ url: '/mock_possible_cities', method: "get", absoluteURL: () => "" });
jsRoutes.controllers.GithubConfigController.index = () => ({ url: '/mock_github_config', method: "get", absoluteURL: () => "" });
jsRoutes.controllers.web.settings.EnvironmentVariablesController.list = () => ({ url: '/mock_environment_variables_list', method: "get", absoluteURL: () => "" });
jsRoutes.controllers.web.settings.RegionalSettingsController.index = () => ({ url: '/mock_regional_settings', method: "get", absoluteURL: () => "" });
jsRoutes.controllers.web.settings.IntegrationsController.list = () => ({ url: '/mock_integrations', method: "get", absoluteURL: () => "" });

describe('RegionalSettings', () => {
  const onSaveTimeZone = jest.fn();

  const defaultConfig: RegionalSettingsProps = {
    csrfToken: "0",
    teamId: "1",
    isAdmin: false,
    onSaveTimeZone: onSaveTimeZone,
    teamTimeZone: "America/New_York",
    teamTimeZoneName: "Eastern Time",
    teamTimeZoneOffset: -14400
  };

  function createIndex(config: RegionalSettingsProps): RegionalSettings {
    const page: Page = TestUtils.renderIntoDocument(
      <Page csrfToken={config.csrfToken} feedbackContainer={document.createElement('span')}
        onRender={(pageProps) => (
          <RegionalSettings {...config} {...pageProps} />
        )}
      />
    ) as Page;
    return page.component as RegionalSettings;
  }

  let config: RegionalSettingsProps;

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
