// @flow
import type {Diffable, DiffableProp} from "./diffs";

import ApiConfigRef from './api_config_ref';
import RequiredApiConfigWithConfig from './required_api_config_with_config';
import ID from '../lib/id';
/* eslint-disable no-use-before-define */
type callback = () => void

type AWSEditor = {
  onAddAWSConfig: (RequiredAWSConfig, ?callback) => void,
  addNewAWSConfig: (?RequiredAWSConfig) => void,
  onRemoveAWSConfig: (RequiredAWSConfig, ?callback) => void,
  onUpdateAWSConfig: (RequiredAWSConfig, ?callback) => void,
  getAllAWSConfigs: () => Array<AWSConfigRef>,
  getOAuth2ApiNameForConfig: (AWSConfigRef) => string
}

type RequiredAWSConfigProps = {
  id: string,
  exportId: string,
  apiId: string,
  nameInCode: string,
  config: ?AWSConfigRef
}

const logoUrl = "/assets/images/logos/aws_logo_web_300px.png";

class RequiredAWSConfig extends RequiredApiConfigWithConfig implements Diffable {

    static fromJson: (props: RequiredAWSConfigProps) => RequiredAWSConfig;

    diffProps(): Array<DiffableProp> {
      return [{
        name: "Name used in code",
        value: this.nameInCode || ""
      }, {
        name: "Configuration to use",
        value: this.configName()
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

    clone(props: RequiredAWSConfigProps): RequiredAWSConfig {
      return RequiredAWSConfig.fromProps((Object.assign({}, this, props)));
    }

    static fromProps(props) {
      return new RequiredAWSConfig(props.id, props.exportId, props.apiId, props.nameInCode, props.config);
    }

  }

  class AWSConfigRef extends ApiConfigRef {

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

    static fromJson(props): AWSConfigRef {
      return new AWSConfigRef(props.id, props.displayName);
    }
}

RequiredAWSConfig.fromJson = function(props): RequiredAWSConfig {
  const config = props.config ? AWSConfigRef.fromJson(props.config) : null;
  return new RequiredAWSConfig(props.id, props.exportId, props.apiId, props.nameInCode, config);
};

export {AWSConfigRef, RequiredAWSConfig};
