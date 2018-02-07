import * as React from 'react';
import ApiConfigList from './api_config_list';
import BehaviorSwitcherGroup from './behavior_switcher_group';
import BehaviorVersion from '../models/behavior_version';
import LibraryVersion from '../models/library_version';
import NodeModuleVersion from '../models/node_module_version';
import {RequiredAWSConfig} from '../models/aws';
import {RequiredOAuth2Application} from '../models/oauth2';
import {RequiredSimpleTokenApi} from '../models/simple_token';

const BehaviorSwitcher = React.createClass({
    propTypes: {
      actionBehaviors: React.PropTypes.arrayOf(React.PropTypes.instanceOf(BehaviorVersion)).isRequired,
      dataTypeBehaviors: React.PropTypes.arrayOf(React.PropTypes.instanceOf(BehaviorVersion)).isRequired,
      libraries: React.PropTypes.arrayOf(React.PropTypes.instanceOf(LibraryVersion)).isRequired,
      nodeModuleVersions: React.PropTypes.arrayOf(React.PropTypes.instanceOf(NodeModuleVersion)).isRequired,
      selectedId: React.PropTypes.string,
      groupId: React.PropTypes.string,
      groupName: React.PropTypes.string.isRequired,
      groupDescription: React.PropTypes.string.isRequired,
      onSelect: React.PropTypes.func.isRequired,
      addNewAction: React.PropTypes.func.isRequired,
      addNewDataType: React.PropTypes.func.isRequired,
      addNewLibrary: React.PropTypes.func.isRequired,
      isModified: React.PropTypes.func.isRequired,
      onUpdateNodeModules: React.PropTypes.func.isRequired,
      requiredAWSConfigs: React.PropTypes.arrayOf(React.PropTypes.instanceOf(RequiredAWSConfig)).isRequired,
      requiredOAuth2Applications: React.PropTypes.arrayOf(React.PropTypes.instanceOf(RequiredOAuth2Application)).isRequired,
      requiredSimpleTokenApis: React.PropTypes.arrayOf(React.PropTypes.instanceOf(RequiredSimpleTokenApi)).isRequired,
      onApiConfigClick: React.PropTypes.func.isRequired,
      onAddApiConfigClick: React.PropTypes.func.isRequired,
      getApiConfigName: React.PropTypes.func.isRequired
    },

    getSkillTitle: function() {
      return (
        <div className="mbxs">
          <h4 className="mvn">{this.props.groupName}</h4>
          <div className="type-s type-weak display-ellipsis display-limit-width"
            title={this.props.groupDescription}>
            {this.props.groupDescription}
          </div>
        </div>
      );
    },

    onEditSkillDetails: function() {
      this.props.onSelect(this.props.groupId, null);
    },

    getAllBehaviors: function() {
      return this.props.actionBehaviors.concat(this.props.dataTypeBehaviors);
    },

    getEditables: function() {
      return this.getAllBehaviors().concat(this.props.libraries);
    },

    getSelected: function() {
      return this.getEditables().find(ea => ea.getPersistentId() === this.props.selectedId);
    },

    renderNodeModules: function() {
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
                  className={`pvxs`}
                >
                  <div className="phxl mobile-phl type-monospace">
                    <span>{version.from}</span>
                    <span className="type-weak"> —&nbsp;v{version.version}</span></div>
                </div>
              ))}
            </div>
            <div className="container container-wide mvm">
              <button type="button"
                      onClick={this.props.onUpdateNodeModules}
                      className="button button-s button-shrink">Update NPM module versions</button>
            </div>
          </div>
        );
      } else {
        return null;
      }
    },

    render: function() {
      return (
        <div className="pbxxxl">

          <div className="border-bottom ptxl pbl">

            <div className="container container-wide">
              {this.getSkillTitle()}
            </div>

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
});

export default BehaviorSwitcher;