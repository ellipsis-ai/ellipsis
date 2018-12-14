import * as React from 'react';
import AddButton from '../form/add_button';
import Button from '../form/button';
import {RequiredAWSConfig} from '../models/aws';
import {RequiredOAuthApplication} from '../models/oauth';
import {RequiredSimpleTokenApi} from '../models/simple_token';
import autobind from "../lib/autobind";
import RequiredApiConfig from "../models/required_api_config";

interface Props {
  requiredAWSConfigs: Array<RequiredAWSConfig>,
  requiredOAuthApplications: Array<RequiredOAuthApplication>,
  requiredSimpleTokenApis: Array<RequiredSimpleTokenApi>,
  onApiConfigClick: (config: RequiredApiConfig) => void,
  onAddApiConfigClick: () => void,
  getApiConfigName: (config: RequiredApiConfig) => string
}

class ApiConfigList extends React.Component<Props> {
    constructor(props: Props) {
      super(props);
      autobind(this);
    }

    onApiConfigClick(required: RequiredApiConfig): void {
      this.props.onApiConfigClick(required);
    }

    renderConfig(required: RequiredApiConfig) {
      const name = this.props.getApiConfigName(required);
      const path = required.codePath();
      const onClick = this.onApiConfigClick.bind(this, required);
      return (
        <div className="plxl mobile-pll">
          <Button onClick={onClick} className="button-block">
            <span className="link">{name}</span>
            <span className="type-pink type-bold type-italic">
              {required.isConfigured() ? null : " — Unconfigured"}
            </span>
          </Button>
          <div className="display-limit-width display-overflow-hidden" title={path}>
            <code className="type-weak">{path}</code>
          </div>
        </div>
      );
    }

    renderConfigs(configs: Array<RequiredApiConfig>) {
      return configs.map((required, index) => (
        <div key={`apiConfig${index}`} className={`pvxs`}>
          {this.renderConfig(required)}
        </div>
      ));
    }

    render() {
      const awsConfigs = this.renderConfigs(this.props.requiredAWSConfigs);
      const oAuthConfigs = this.renderConfigs(this.props.requiredOAuthApplications);
      const simpleTokenConfigs = this.renderConfigs(this.props.requiredSimpleTokenApis);
      const hasConfigs = awsConfigs.length > 0 || oAuthConfigs.length > 0 || simpleTokenConfigs.length > 0;
      return (
        <div className="border-bottom pbl">
          <div className="container container-wide prl">
            <div className="columns columns-elastic">
              <div className="column column-expand ptl">
                <h6>API integrations</h6>
              </div>
              <div className="column column-shrink ptm type-link">
                <AddButton
                  onClick={this.props.onAddApiConfigClick}
                  label={"Add new configuration"}
                />
              </div>
            </div>
          </div>
          <div className={`type-s ${hasConfigs ? "mts" : ""}`}>
            {awsConfigs}
            {oAuthConfigs}
            {simpleTokenConfigs}
          </div>
        </div>
      );
    }
}

export default ApiConfigList;
