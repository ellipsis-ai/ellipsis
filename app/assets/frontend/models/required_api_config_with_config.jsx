// @flow
import ApiConfigRef from './api_config_ref';
import RequiredApiConfig from './required_api_config';

class RequiredApiConfigWithConfig extends RequiredApiConfig {
    config: ApiConfigRef;

    constructor(id: string, exportId: string, apiId: string, nameInCode: string, config: ApiConfigRef) {
      super(id, exportId, apiId, nameInCode);
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

    canHaveConfig(): boolean {
      return true;
    }
  }

export default RequiredApiConfigWithConfig;
