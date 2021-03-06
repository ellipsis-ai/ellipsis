import * as React from 'react';
import ApiConfigList from './api_config_list';
import BehaviorSwitcherGroup from './behavior_switcher_group';
import BehaviorVersion from '../models/behavior_version';
import LibraryVersion from '../models/library_version';
import NodeModuleVersion from '../models/node_module_version';
import {RequiredAWSConfig} from '../models/aws';
import {RequiredOAuthApplication} from '../models/oauth';
import {RequiredSimpleTokenApi} from '../models/simple_token';
import DynamicLabelButton from "../form/dynamic_label_button";
import Editable from "../models/editable";
import RequiredApiConfig from "../models/required_api_config";
import autobind from "../lib/autobind";
import BehaviorTestResult from "../models/behavior_test_result";
import TestsStatus from "./tests_status";
import {ReactNode} from "react";
import SVGCheckmark from '../svg/checkmark';
import SVGWarning from '../svg/warning';
import NodeModuleList from "./node_module_list";

export type BehaviorSelectCallback = (optionalGroupId: Option<string>, editableId?: Option<string>, optionalCallback?: () => void) => void;

interface Props {
  actionBehaviors: Array<BehaviorVersion>,
  dataTypeBehaviors: Array<BehaviorVersion>,
  libraries: Array<LibraryVersion>,
  tests: Array<BehaviorVersion>,
  nodeModuleVersions: Array<NodeModuleVersion>,
  selectedId?: Option<string>,
  groupId: Option<string>,
  onSelect: BehaviorSelectCallback,
  addNewAction: () => void,
  addNewDataType: () => void,
  addNewTest: () => void,
  addNewLibrary: () => void,
  isModified: (editable: Editable) => boolean,
  onUpdateNodeModules: () => void,
  requiredAWSConfigs: Array<RequiredAWSConfig>,
  requiredOAuthApplications: Array<RequiredOAuthApplication>,
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
      return this.props.actionBehaviors.concat(this.props.dataTypeBehaviors).concat(this.props.tests);
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
              <h6>{NodeModuleVersion.icon()} NPM modules</h6>
            </div>
            <div className="type-s">
              <NodeModuleList nodeModuleVersions={this.props.nodeModuleVersions} updatingNodeModules={this.props.updatingNodeModules} />
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

    renderTestsStatus(): ReactNode {
      return (
        <TestsStatus
          isRunning={!!this.props.runningTests}
          testResults={this.props.testResults}
        />
      );
    }

    renderTestStatusFor(test: Editable): ReactNode {
      const result = (this.props.testResults || []).find(ea => ea.behaviorVersionId === test.id);
      if (result) {
        if (result.isPass) {
          return (
            <div className="display-inline-block fade-in">
              <span className="display-inline-block height-l mrs align-m type-green">
                <SVGCheckmark label="Pass"/>
              </span>
            </div>
          );
        } else {
          return (
            <div className="display-inline-block fade-in">
              <span className="display-inline-block align-m height-l type-pink mrs">
                <SVGWarning label="Fail"/>
              </span>
            </div>
          );
        }
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
              heading={`${BehaviorVersion.actionIcon()} Actions`}
              editables={this.props.actionBehaviors}
              selectedId={this.props.selectedId}
              onAddNew={this.props.addNewAction}
              addNewLabel="Add new action"
              onSelect={this.props.onSelect}
              isModified={this.props.isModified}
            />

            <BehaviorSwitcherGroup
              heading={`${BehaviorVersion.dataTypeIcon()} Data types`}
              editables={this.props.dataTypeBehaviors}
              selectedId={this.props.selectedId}
              onAddNew={this.props.addNewDataType}
              addNewLabel="Add new data type"
              onSelect={this.props.onSelect}
              isModified={this.props.isModified}
            />

            <BehaviorSwitcherGroup
              heading={`${LibraryVersion.icon()} Libraries`}
              editables={this.props.libraries}
              selectedId={this.props.selectedId}
              onAddNew={this.props.addNewLibrary}
              addNewLabel="Add new library"
              onSelect={this.props.onSelect}
              isModified={this.props.isModified}
            />

            <BehaviorSwitcherGroup
              heading={`${BehaviorVersion.testIcon()} Tests`}
              editables={this.props.tests}
              selectedId={this.props.selectedId}
              onAddNew={this.props.addNewTest}
              addNewLabel="Add new test"
              onSelect={this.props.onSelect}
              isModified={this.props.isModified}
              renderGroupStatus={this.renderTestsStatus}
              renderEditableStatus={this.renderTestStatusFor}
            />

            <ApiConfigList
              requiredAWSConfigs={this.props.requiredAWSConfigs}
              requiredOAuthApplications={this.props.requiredOAuthApplications}
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
