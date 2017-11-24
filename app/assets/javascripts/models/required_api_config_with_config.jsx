// @flow
define(function(require) {
  const ApiConfigRef = require('./api_config_ref');
  const RequiredApiConfig = require('./required_api_config');

  class RequiredApiConfigWithConfig extends RequiredApiConfig {
    config: ApiConfigRef;

    constructor(id: string, apiId: string, nameInCode: string, config: ApiConfigRef) {
      super(id, apiId, nameInCode);
      Object.defineProperties(this, {
        config: {value: config, enumerable: true}
      });
    }

    canHaveConfig(): boolean {
      return true;
    }
  }

  return RequiredApiConfigWithConfig;

});
