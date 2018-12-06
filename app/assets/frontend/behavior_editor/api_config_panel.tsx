import * as React from 'react';
import ApiConfigRef from '../models/api_config_ref';
import DropdownMenu from '../shared_ui/dropdown_menu';
import Collapsible from '../shared_ui/collapsible';
import FormInput from '../form/input';
import Button from '../form/button';
import RequiredApiConfig from '../models/required_api_config';
import Select from '../form/select';
import Sort from '../lib/sort';
import autobind from "../lib/autobind";
import BehaviorEditor from "./index";

export interface ApiConfigEditor<R extends RequiredApiConfig> {
  allApiConfigsFor: Array<ApiConfigRef>,
  onGetApiLogoUrl: (apiId: string) => string,
  onGetApiName: (apiId: string) => string,
  onAddConfig: (required: R, callback?: () => void) => void,
  onAddNewConfig: (required?: R, callback?: () => void) => void,
  onRemoveConfig: (required: R, callback?: () => void) => void,
  onUpdateConfig: (required: R, callback?: () => void) => void
}

interface Props<R extends RequiredApiConfig> {
  openWhen: boolean,
  requiredConfig: Option<R>,
  allConfigs: Array<ApiConfigRef>,
  toggle: () => void,
  onDoneClick: () => void,
  addNewAWSConfig: () => void,
  addNewOAuthApplication: () => void,
  animationDisabled?: boolean,
  editor: BehaviorEditor,
  onAddNewConfig: (required: RequiredApiConfig) => void
}

const ADD_NEW_CONFIG_KEY = "add_new_config";

class ApiConfigPanel<R extends RequiredApiConfig> extends React.Component<Props<R>> {
    constructor(props: Props<R>) {
      super(props);
      autobind(this);
    }

    getAllConfigs(): Array<ApiConfigRef> {
      const configs = this.props.requiredConfig ?
        this.props.requiredConfig.editorFor(this.props.editor).allApiConfigsFor : this.props.allConfigs;
      return Sort.arrayAlphabeticalBy(configs, (ea) => ea.displayName);
    }

    getSelectorLabelForConfig(config: ApiConfigRef) {
      const editor = config.editorFor(this.props.editor);
      const url = editor.onGetApiLogoUrl(config.getApiId());
      const name = config.configName();
      return (
        <div className="columns columns-elastic">
          {url ? (
            <div className="column column-shrink prs align-m">
              <img src={url} height="24" alt={`Logo for ${name}`} />
            </div>
          ) : null}
          <div className="column column-expand align-m">
            {name}
          </div>
        </div>
      );
    }

    onAddNewRequiredFor(config: ApiConfigRef): void {
      const newRequired = config.newRequired() as R;
      config.editorFor(this.props.editor).onAddConfig(newRequired);
      this.props.onAddNewConfig(newRequired);
    }

    onDeleteRequired(): void {
      const required = this.props.requiredConfig;
      if (required) {
        required.editorFor(this.props.editor).onRemoveConfig(required);
        this.props.onDoneClick();
      }
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

    renderConfigChoice(required: R) {
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
      const existingRequired = this.props.requiredConfig;
      if (existingRequired) {
        const editor = existingRequired.editorFor(this.props.editor);
        if (newConfigId === ADD_NEW_CONFIG_KEY) {
          editor.onAddNewConfig();
        } else {
          const newConfig = this.getAllConfigs().find(ea => ea.id === newConfigId);
          if (existingRequired && newConfig) {
            const newRequired = existingRequired.clone({
              config: newConfig
            });
            editor.onUpdateConfig(newRequired);
          }
        }
      }
    }

    onNameInCodeChange(newNameInCode: string): void {
      const existing = this.props.requiredConfig;
      if (existing) {
        const requiredConfig = existing.clone({
          nameInCode: newNameInCode
        });
        existing.editorFor(this.props.editor).onUpdateConfig(requiredConfig);
      }
    }

    renderNameInCode(required: R) {
      return (
        <div className="type-s">
          <h5 className="mtn position-relative"><span>Set code path</span></h5>
          <p className="mbxs">This determines how to access the configuration from code. It should be different from
            any other API integration in this skill.</p>

          <div>
            <div className="align-form-input display-inline-block">
              <span className="type-monospace">{required.codePathPrefix()}</span>
            </div>
            <div className="align-form-input display-inline-block width-20">
              <FormInput
                className="form-input-borderless type-monospace"
                value={required.nameInCode}
                placeholder="nameInCode"
                onChange={this.onNameInCodeChange}
              />
            </div>
          </div>
        </div>
      );
    }

    renderConfig() {
      const required = this.props.requiredConfig;
      const editor = required ? required.editorFor(this.props.editor) : null;
      const imageUrl = required && editor ? editor.onGetApiLogoUrl(required.apiId) : null;
      const name = required && editor ? editor.onGetApiName(required.apiId) : null;
      return (
        <div>
          <Collapsible revealWhen={Boolean(required)} animationDisabled={this.props.animationDisabled}>

            {required ? (
              <div>
                <div className="mbxl">
                  {imageUrl ? (
                    <img className="mrs align-m" src={imageUrl} height="24" alt={`Logo for ${name}`} />
                  ) : null}
                  <span>{name}</span>
                </div>

                <div className="mvxl">
                  {this.renderNameInCode(required)}
                </div>

                <div className="mvxl">
                  {this.renderConfigChoice(required)}
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

