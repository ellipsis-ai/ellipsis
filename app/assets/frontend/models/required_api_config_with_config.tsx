import ApiConfigRef from './api_config_ref';
import RequiredApiConfig from './required_api_config';

abstract class RequiredApiConfigWithConfig extends RequiredApiConfig {
    constructor(
      readonly id: Option<string>,
      readonly exportId: Option<string>,
      readonly apiId: string,
      readonly nameInCode: string,
      readonly config: Option<ApiConfigRef>
    ) {
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
