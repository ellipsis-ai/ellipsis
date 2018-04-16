import {Diffable, DiffableProp} from "./diffs";
import ApiConfigRef, {ApiConfigRefJson} from './api_config_ref';
import RequiredApiConfigWithConfig from './required_api_config_with_config';
import ID from '../lib/id';
import {RequiredApiConfigJson} from "./required_api_config";
type callback = () => void

type AWSEditor = {
  onAddAWSConfig: (r: RequiredAWSConfig, c?: Option<callback>) => void,
  addNewAWSConfig: (r?: RequiredAWSConfig) => void,
  onRemoveAWSConfig: (r: RequiredAWSConfig, c?: Option<callback>) => void,
  onUpdateAWSConfig: (r: RequiredAWSConfig, c?: Option<callback>) => void,
  getAllAWSConfigs: () => Array<AWSConfigRef>,
  getOAuth2ApiNameForConfig: (a: AWSConfigRef) => string
}

export interface RequiredAWSConfigJson extends RequiredApiConfigJson {
  config?: Option<AWSConfigRefJson>
}

interface RequiredAWSConfigInterface extends RequiredAWSConfigJson {
  config?: Option<AWSConfigRef>
}

const logoUrl = "/assets/images/logos/aws_logo_web_300px.png";

class RequiredAWSConfig extends RequiredApiConfigWithConfig implements RequiredAWSConfigInterface, Diffable {
  readonly config: Option<AWSConfigRef>;

  constructor(
    id: Option<string>,
    exportId: Option<string>,
    apiId: string,
    nameInCode: string,
    config: Option<AWSConfigRef>
  ) {
    super(id, exportId, apiId, nameInCode, config);
  }

  diffProps(): Array<DiffableProp> {
      return [{
        name: "Name used in code",
        value: this.nameInCode || "",
        ignoreForPublished: true
      }, {
        name: "Configuration to use",
        value: this.configName(),
        ignoreForPublished: true
      }];
    }

    onAddConfigFor(editor: AWSEditor) {
      return editor.onAddAWSConfig;
    }

    onAddNewConfigFor(editor: AWSEditor) {
      return editor.addNewAWSConfig;
    }

    onRemoveConfigFor(editor: AWSEditor) {
      return editor.onRemoveAWSConfig;
    }

    onUpdateConfigFor(editor: AWSEditor) {
      return editor.onUpdateAWSConfig;
    }

    getApiLogoUrl(): string {
      return logoUrl;
    }

    getApiName(): string {
      return "AWS";
    }

    getAllConfigsFrom(editor: AWSEditor) {
      return editor.getAllAWSConfigs();
    }

    codePathPrefix(): string {
      return "ellipsis.aws.";
    }

    codePath(): string {
      return `${this.codePathPrefix()}${this.nameInCode}`;
    }

    configName(): string {
      return this.config ? this.config.displayName : "";
    }

    isConfigured(): boolean {
      return Boolean(this.config);
    }

    clone(props: Partial<RequiredAWSConfigInterface>): RequiredAWSConfig {
      return RequiredAWSConfig.fromProps(Object.assign({}, this, props));
    }

    static fromProps(props: RequiredAWSConfigInterface) {
      return new RequiredAWSConfig(props.id, props.exportId, props.apiId, props.nameInCode, props.config);
    }

    static fromJson(props: RequiredAWSConfigJson): RequiredAWSConfig {
      const config = props.config ? AWSConfigRef.fromJson(props.config) : null;
      return RequiredAWSConfig.fromProps({
        id: props.id,
        exportId: props.exportId,
        apiId: props.apiId,
        nameInCode: props.nameInCode,
        config: config
      });
    }

}

  export interface AWSConfigRefJson extends ApiConfigRefJson {}

  class AWSConfigRef extends ApiConfigRef implements AWSConfigRefJson {

    newRequired(): RequiredAWSConfig {
      return new RequiredAWSConfig(
        ID.next(),
        ID.next(),
        'aws',
        this.defaultNameInCode(),
        this
      );
    }

    getApiLogoUrl(): string {
      return logoUrl;
    }

    getApiName(editor: AWSEditor): string {
      return editor.getOAuth2ApiNameForConfig(this);
    }

    configName(): string {
      return this.displayName;
    }

    static fromJson(props: AWSConfigRefJson): AWSConfigRef {
      return new AWSConfigRef(props.id, props.displayName);
    }
}

export {AWSConfigRef, RequiredAWSConfig};
