import * as React from 'react';
import ifPresent from '../lib/if_present';
import ApiConfigRef from '../models/api_config_ref';
import DropdownMenu from '../shared_ui/dropdown_menu';
import Collapsible from '../shared_ui/collapsible';
import FormInput from '../form/input';
import Button from '../form/button';
import RequiredApiConfig from '../models/required_api_config';
import Select from '../form/select';
import Sort from '../lib/sort';
import autobind from "../lib/autobind";
import RequiredApiConfigWithConfig from "../models/required_api_config_with_config";

interface Props {
  openWhen: boolean,
  requiredConfig: RequiredApiConfig,
  allConfigs: Array<ApiConfigRef>,
  onAddConfig: (config: RequiredApiConfig) => void,
  onAddNewConfig: (config: RequiredApiConfig) => void,
  onRemoveConfig: (config: RequiredApiConfig) => void,
  onUpdateConfig: (config: RequiredApiConfig) => void,
  getApiLogoUrlForConfig: (config: RequiredApiConfigWithConfig | ApiConfigRef) => string,
  getApiNameForConfig: (config: RequiredApiConfigWithConfig) => string,
  getApiConfigName: (config: ApiConfigRef) => string,
  toggle: () => void,
  onDoneClick: () => void,
  addNewAWSConfig: () => void,
  addNewOAuthApplication: () => void,
  animationDisabled?: boolean
}

const ADD_NEW_CONFIG_KEY = "add_new_config";

class ApiConfigPanel extends React.Component<Props> {
    constructor(props: Props) {
      super(props);
      autobind(this);
    }

    getAllConfigs(): Array<ApiConfigRef> {
      return Sort.arrayAlphabeticalBy(this.props.allConfigs, (ea) => ea.displayName);
    }

    getApiLogoUrlForConfig(config: RequiredApiConfigWithConfig | ApiConfigRef): string {
      return this.props.getApiLogoUrlForConfig(config);
    }

    getSelectorLabelForConfig(config: ApiConfigRef) {
      return (
        <div className="columns columns-elastic">
          {ifPresent(this.getApiLogoUrlForConfig(config), url => {
            return (
              <div className="column column-shrink prs align-m">
                <img src={url} height="24"/>
              </div>
            );
          })}
          <div className="column column-expand align-m">
            {this.props.getApiConfigName(config)}
          </div>
        </div>
      );
    }

    onAddNewRequiredFor(config: ApiConfigRef): void {
      this.props.onAddConfig(config.newRequired());
    }

    onDeleteRequired(): void {
      this.props.onRemoveConfig(this.props.requiredConfig);
      this.props.onDoneClick();
    }

    render() {
      return (
        <div className="box-action phn">
          <div className="container">
            <div className="columns">
              <div className="column column-page-sidebar">
                <h4 className="type-weak mtn">Configure an API integration</h4>
              </div>
              <div className="column column-page-main">
                {this.renderConfig()}

                {this.renderButtons()}
              </div>
            </div>
          </div>
        </div>
      );
    }

    renderAdder() {
      return (
        <DropdownMenu
          openWhen={this.props.openWhen}
          toggle={this.props.toggle}
          label="Select an API to use…"
          labelClassName="button-dropdown-trigger-menu-above"
          menuClassName="popup-dropdown-menu-wide popup-dropdown-menu-above"
        >
          {this.getAllConfigs().map((cfg, index) => (
            <DropdownMenu.Item
              key={"api-config-" + index}
              onClick={this.onAddNewRequiredFor.bind(this, cfg)}
              label={this.getSelectorLabelForConfig(cfg)}
            />
          ))}
          <DropdownMenu.Item
            onClick={this.props.addNewAWSConfig}
            className="border-top"
            label="Add new AWS configuration…"
          />
          <DropdownMenu.Item
            onClick={this.props.addNewOAuthApplication}
            label="Add new OAuth API configuration…"
          />
        </DropdownMenu>
      );
    }

    renderConfigChoice() {
      const required = this.props.requiredConfig;
      if (required.canHaveConfig()) {
        return (
          <div>
            <h5 className="mtn position-relative">
              <span>Set API configuration to use</span>
            </h5>
            <p className="type-s">These configurations can be shared across skills.</p>
            <div>
              <Select
                className="form-select-s form-select-light align-m mrm mbs"
                name="paramType"
                value={required.config ? required.config.id : undefined}
                onChange={this.onConfigChange}
              >
                <option key="config-choice-none">None selected</option>
                {this.getAllConfigs().map(ea => (
                  <option key={`config-choice-${ea.id}`} value={ea.id}>{ea.displayName}</option>
                ))}
                <option key="config-choice-new" value={ADD_NEW_CONFIG_KEY}>Add a new configuration…</option>
              </Select>
            </div>
          </div>
        );
      } else {
        return null;
      }
    }

    onConfigChange(newConfigId: string): void {
      if (newConfigId === ADD_NEW_CONFIG_KEY) {
        this.props.onAddNewConfig(this.props.requiredConfig);
      } else {
        const newConfig = this.getAllConfigs().find(ea => ea.id === newConfigId);
        if (newConfig) {
          this.props.onUpdateConfig(this.props.requiredConfig.clone({
            config: newConfig
          }));
        }
      }
    }

    onNameInCodeChange(newNameInCode: string): void {
      this.props.onUpdateConfig(this.props.requiredConfig.clone({
        nameInCode: newNameInCode
      }));
    }

    renderNameInCode() {
      return (
        <div className="type-s">
          <h5 className="mtn position-relative"><span>Set code path</span></h5>
          <p className="mbxs">This determines how to access the configuration from code. It should be different from
            any other API integration in this skill.</p>

          <div>
            <div className="align-form-input display-inline-block">
              <span className="type-monospace">{this.props.requiredConfig.codePathPrefix()}</span>
            </div>
            <div className="align-form-input display-inline-block width-20">
              <FormInput
                className="form-input-borderless type-monospace"
                value={this.props.requiredConfig.nameInCode}
                placeholder="nameInCode"
                onChange={this.onNameInCodeChange}
              />
            </div>
          </div>
        </div>
      );
    }

    renderConfig() {
      const hasConfig = Boolean(this.props.requiredConfig);
      const imageUrl = hasConfig ? this.getApiLogoUrlForConfig(this.props.requiredConfig) : null;
      const name = this.props.getApiNameForConfig(this.props.requiredConfig);
      return (
        <div>
          <Collapsible revealWhen={hasConfig} animationDisabled={this.props.animationDisabled}>

            {hasConfig ? (
              <div>
                <div className="mbxl">
                  {imageUrl ? (
                    <img className="mrs align-m" src={imageUrl} height="24" alt={`Logo for ${name}`} />
                  ) : null}
                  <span>{name}</span>
                </div>

                <div className="mvxl">
                  {this.renderNameInCode()}
                </div>

                <div className="mvxl">
                  {this.renderConfigChoice()}
                </div>
              </div>
            ) : (
              <div/>
            )}
          </Collapsible>

          <Collapsible revealWhen={!this.props.requiredConfig} animationDisabled={this.props.animationDisabled}>
            {this.renderAdder()}
          </Collapsible>
        </div>
      );
    }

    renderButtons() {
      return (
        <div className="mtxl">
          <Button className="button-primary mbs mrs" onClick={this.props.onDoneClick}>
            {this.props.requiredConfig ? "Done" : "Cancel"}
          </Button>
          {this.props.requiredConfig ? (
            <Button className="button mbs" onClick={this.onDeleteRequired}>Remove integration from skill</Button>
          ) : null}
        </div>
      );
    }

}

export default ApiConfigPanel;

