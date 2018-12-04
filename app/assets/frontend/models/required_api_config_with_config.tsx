import ApiConfigRef from './api_config_ref';
import RequiredApiConfig from './required_api_config';

abstract class RequiredApiConfigWithConfig extends RequiredApiConfig {
    constructor(id: Option<string>, exportId: Option<string>, apiId: string, nameInCode: string, config: Option<ApiConfigRef>) {
      super(id, exportId, apiId, nameInCode);
      Object.defineProperties(this, {
        config: {value: config, enumerable: true}
      });
    }

    configName(): Option<string> {
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
