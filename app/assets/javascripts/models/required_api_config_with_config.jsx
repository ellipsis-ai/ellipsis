// @flow
define(function(require) {
  const ApiConfigRef = require('./api_config_ref');
  const diffs = require('./diffs');
  const RequiredApiConfig = require('./required_api_config');

  class RequiredApiConfigWithConfig extends RequiredApiConfig {
    config: ApiConfigRef;

    constructor(id: string, requiredId: string, apiId: string, nameInCode: string, config: ApiConfigRef) {
      super(id, requiredId, apiId, nameInCode);
      Object.defineProperties(this, {
        config: {value: config, enumerable: true}
      });
    }

    configName(): ?string {
      if (this.config) {
        return this.config.displayName;
      } else {
        return null;
      }
    }

    maybeConfigToUseDiffFor(other: RequiredApiConfigWithConfig): ?diffs.CategoricalPropertyDiff {
      return diffs.CategoricalPropertyDiff.maybeFor("Configuration to use", this.configName(), other.configName());
    }

    canHaveConfig(): boolean {
      return true;
    }
  }

  return RequiredApiConfigWithConfig;

});
