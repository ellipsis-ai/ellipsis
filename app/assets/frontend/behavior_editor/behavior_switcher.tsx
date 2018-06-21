import * as React from 'react';
import ApiConfigList from './api_config_list';
import BehaviorSwitcherGroup from './behavior_switcher_group';
import BehaviorVersion from '../models/behavior_version';
import LibraryVersion from '../models/library_version';
import NodeModuleVersion from '../models/node_module_version';
import {RequiredAWSConfig} from '../models/aws';
import {RequiredOAuth2Application} from '../models/oauth2';
import {RequiredSimpleTokenApi} from '../models/simple_token';
import DynamicLabelButton from "../form/dynamic_label_button";
import Editable from "../models/editable";
import RequiredApiConfig from "../models/required_api_config";
import autobind from "../lib/autobind";
import BehaviorTestResult from "../models/behavior_test_result";
import TestsStatus from "./tests_status";

interface Props {
  actionBehaviors: Array<BehaviorVersion>,
  dataTypeBehaviors: Array<BehaviorVersion>,
  libraries: Array<LibraryVersion>,
  tests: Array<BehaviorVersion>,
  nodeModuleVersions: Array<NodeModuleVersion>,
  selectedId?: Option<string>,
  groupId: string,
  onSelect: (groupId: string, editableId?: Option<string>) => void,
  addNewAction: () => void,
  addNewDataType: () => void,
  addNewTest: () => void,
  addNewLibrary: () => void,
  isModified: (editable: Editable) => boolean,
  onUpdateNodeModules: () => void,
  requiredAWSConfigs: Array<RequiredAWSConfig>,
  requiredOAuth2Applications: Array<RequiredOAuth2Application>,
  requiredSimpleTokenApis: Array<RequiredSimpleTokenApi>,
  onApiConfigClick: (config: RequiredApiConfig) => void,
  onAddApiConfigClick: () => void,
  getApiConfigName: (config: RequiredApiConfig) => string,
  updatingNodeModules: boolean,
  runningTests: boolean,
  testResults: Array<BehaviorTestResult>
}

class BehaviorSwitcher extends React.Component<Props> {
    constructor(props: Props) {
      super(props);
      autobind(this);
    }

    onEditSkillDetails() {
      this.props.onSelect(this.props.groupId, null);
    }

    getAllBehaviors(): Array<Editable> {
      return this.props.actionBehaviors.concat(this.props.dataTypeBehaviors);
    }

    getEditables(): Array<Editable> {
      return this.getAllBehaviors().concat(this.props.libraries);
    }

    getSelected(): Option<Editable> {
      return this.getEditables().find(ea => ea.getPersistentId() === this.props.selectedId);
    }

    renderNodeModules() {
      if (this.props.nodeModuleVersions.length) {
        return (
          <div className="border-bottom mtl pbl">
            <div className="container container-wide mbs">
              <h6>Required NPM modules</h6>
            </div>
            <div className="type-s">
              {this.props.nodeModuleVersions.map((version, index) => (
                <div
                  key={`nodeModuleVersion${index}`}
                  className={`pbxs`}
                >
                  <div className="phxl mobile-phl type-monospace display-ellipsis" title={
                    `${version.from} v${version.version}`
                  }>
                    <span>{version.from}</span>
                    {this.props.updatingNodeModules ? (
                      <span className="pulse type-disabled">@...</span>
                    ) : (
                      <span>
                        <span className="type-disabled">@</span>
                        <span className="type-weak">{version.version}</span>
                      </span>
                    )}
                  </div>
                </div>
              ))}
            </div>
            <div className="container container-wide mvm">
              <DynamicLabelButton
                onClick={this.props.onUpdateNodeModules}
                className="button button-s button-shrink"
                disabledWhen={this.props.updatingNodeModules}
                labels={[{
                  text: "Update NPM module versions",
                  displayWhen: !this.props.updatingNodeModules
                }, {
                  text: "Updating…",
                  displayWhen: this.props.updatingNodeModules
                }]}
              />
            </div>
          </div>
        );
      } else {
        return null;
      }
    }

    render() {
      return (
        <div className="pbxxxl">

          <div className="border-bottom ptxl pbl">

            <div>
              <button type="button"
                className={
                  "button-block pvxs phxl mobile-phl width width-full " +
                  (this.getSelected() ? "" : "bg-blue border-blue-medium type-white")
                }
                onClick={this.onEditSkillDetails}
                disabled={!this.getSelected()}
              >
                <span className={`type-s ${this.getSelected() ? "link" : "type-white"}`}>Skill details</span>
              </button>
            </div>
          </div>

          <div>
            <BehaviorSwitcherGroup
              heading="Actions"
              editables={this.props.actionBehaviors}
              selectedId={this.props.selectedId}
              onAddNew={this.props.addNewAction}
              addNewLabel="Add new action"
              onSelect={this.props.onSelect}
              isModified={this.props.isModified}
            />

            <BehaviorSwitcherGroup
              heading="Data types"
              editables={this.props.dataTypeBehaviors}
              selectedId={this.props.selectedId}
              onAddNew={this.props.addNewDataType}
              addNewLabel="Add new data type"
              onSelect={this.props.onSelect}
              isModified={this.props.isModified}
            />

            <BehaviorSwitcherGroup
              heading="Libraries"
              editables={this.props.libraries}
              selectedId={this.props.selectedId}
              onAddNew={this.props.addNewLibrary}
              addNewLabel="Add new library"
              onSelect={this.props.onSelect}
              isModified={this.props.isModified}
            />

            <BehaviorSwitcherGroup
              heading="Tests"
              editables={this.props.tests}
              selectedId={this.props.selectedId}
              onAddNew={this.props.addNewTest}
              addNewLabel="Add new test"
              onSelect={this.props.onSelect}
              isModified={this.props.isModified}
              isTests={true}
              runningTests={this.props.runningTests}
              testResults={this.props.testResults}
            />

            <ApiConfigList
              requiredAWSConfigs={this.props.requiredAWSConfigs}
              requiredOAuth2Applications={this.props.requiredOAuth2Applications}
              requiredSimpleTokenApis={this.props.requiredSimpleTokenApis}
              onApiConfigClick={this.props.onApiConfigClick}
              onAddApiConfigClick={this.props.onAddApiConfigClick}
              getApiConfigName={this.props.getApiConfigName}
            />

            {this.renderNodeModules()}

          </div>

        </div>
      );
    }
}

export default BehaviorSwitcher;
