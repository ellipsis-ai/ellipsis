import {Diffable, DiffableProp} from "./diffs";
import ApiConfigRef, {ApiConfigRefJson} from './api_config_ref';
import RequiredApiConfigWithConfig from './required_api_config_with_config';
import ID from '../lib/id';
import {RequiredApiConfigJson} from "./required_api_config";
import BehaviorEditor from "../behavior_editor";
import {ApiConfigEditor} from "../behavior_editor/api_config_panel";

export interface RequiredAWSConfigJson extends RequiredApiConfigJson {
  config?: Option<AWSConfigRefJson>
}

interface RequiredAWSConfigInterface extends RequiredAWSConfigJson {
  config?: Option<AWSConfigRef>
}

const logoUrl = "/assets/images/logos/aws_logo_web_300px.png";

class RequiredAWSConfig
  extends RequiredApiConfigWithConfig
  implements RequiredAWSConfigInterface, Diffable {

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
        value: this.configName() || "",
        ignoreForPublished: true
      }];
    }

    codePathPrefix(): string {
      return "ellipsis.aws.";
    }

    codePath(): string {
      return `${this.codePathPrefix()}${this.nameInCode}`;
    }

    configName(): Option<string> {
      return this.config ? this.config.displayName : "";
    }

    isConfigured(): boolean {
      return Boolean(this.config);
    }

    clone(props: Partial<RequiredAWSConfig>) {
      const oldOne: RequiredAWSConfigInterface = Object.assign({}, this);
      const newOne: RequiredAWSConfigInterface = Object.assign(oldOne, props);
      return new RequiredAWSConfig(newOne.id, newOne.exportId, newOne.apiId, newOne.nameInCode, newOne.config) as this;
    }

    static getApiName(): string {
      return "AWS";
    }

    static getApiLogo(): string {
      return logoUrl;
    }

    static fromProps(props: RequiredAWSConfigInterface): RequiredAWSConfig {
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

    editorFor(editor: BehaviorEditor) {
      return RequiredAWSConfig.editorFor(editor);
    }

    static editorFor(editor: BehaviorEditor): ApiConfigEditor<RequiredAWSConfig> {
      return {
        allApiConfigsFor: editor.getAllAWSConfigs(),
        onGetApiLogoUrl: RequiredAWSConfig.getApiLogo,
        onGetApiName: RequiredAWSConfig.getApiName,
        onAddConfig: editor.onAddAWSConfig,
        onAddNewConfig: editor.addNewAWSConfig,
        onRemoveConfig: editor.onRemoveAWSConfig,
        onUpdateConfig: editor.onUpdateAWSConfig,
      }
    }

}

  export interface AWSConfigRefJson extends ApiConfigRefJson {}

  class AWSConfigRef extends ApiConfigRef implements AWSConfigRefJson {
    constructor(
      readonly id: string,
      readonly displayName: string
    ) {
      super(id, displayName);
      this.apiId = id;
    }

    newRequired(): RequiredAWSConfig {
      return new RequiredAWSConfig(
        ID.next(),
        ID.next(),
        'aws',
        this.defaultNameInCode(),
        this
      );
    }

    configName(): string {
      return this.displayName;
    }

    editorFor(editor: BehaviorEditor) {
      return RequiredAWSConfig.editorFor(editor);
    }

    static fromJson(props: AWSConfigRefJson): AWSConfigRef {
      return new AWSConfigRef(props.id, props.displayName);
    }
}

export {AWSConfigRef, RequiredAWSConfig};
