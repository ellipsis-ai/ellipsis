import ApiConfigRef from './api_config_ref';
import RequiredApiConfig from './required_api_config';

class RequiredApiConfigWithConfig extends RequiredApiConfig {
  readonly config: ApiConfigRef | null;

    constructor(id: string, exportId: string, apiId: string, nameInCode: string, config: ApiConfigRef | null) {
      super(id, exportId, apiId, nameInCode);
      Object.defineProperties(this, {
        config: {value: config, enumerable: true}
      });
    }

    configName(): string | null {
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
