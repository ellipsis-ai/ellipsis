// @flow
import * as React from 'react';
import BehaviorGroupModel from '../../javascripts/models/behavior_group';
const BehaviorGroup: any = BehaviorGroupModel; // TODO
import BehaviorVersionModel from '../../javascripts/models/behavior_version';
const BehaviorVersion: any = BehaviorVersionModel;
import EditableName from './editable_name';
import FormatterModule from '../../javascripts/lib/formatter';
const Formatter: any = FormatterModule;
import SVGInstall from '../../javascripts/svg/install';
import SVGInstalled from '../../javascripts/svg/installed';
import ifPresent from '../../javascripts/lib/if_present';
import Sort from '../../javascripts/lib/sort';
import autobind from '../../javascripts/lib/autobind';

  type Props = {
    groupData: ?BehaviorGroup,
    onBehaviorGroupImport: (BehaviorGroup) => void,
    onBehaviorGroupUpdate: (originalGroup: BehaviorGroup, updatedGroup: BehaviorGroup) => void,
    updatedData: ?BehaviorGroup,
    onToggle: () => void,
    isImportable: boolean,
    wasImported: ?boolean,
    localId: ?string
  }

  class BehaviorGroupInfoPanel extends React.Component<Props> {
    constructor(props: Props) {
      super(props);
      autobind(this);
    }

    getBehaviors(): Array<BehaviorVersion> {
      var behaviorVersions = this.props.groupData && this.props.groupData.behaviorVersions || [];
      return Sort.arrayAlphabeticalBy(behaviorVersions.filter((version) => !version.isDataType()), (version) => version.sortKey());
    }

    getName(): React.Node {
      return this.props.groupData && this.props.groupData.name || (
        <span className="type-italic type-disabled">Untitled skill</span>
      );
    }

    onImport(): void {
      if (this.props.groupData) {
        this.props.onBehaviorGroupImport(this.props.groupData);
      }
    }

    onUpdate(): void {
      if (this.props.groupData && this.props.updatedData) {
        this.props.onBehaviorGroupUpdate(this.props.groupData, this.props.updatedData);
      }
    }

    toggle(): void {
      this.props.onToggle();
    }

    render(): React.Node {
      if (!this.props.groupData) {
        return null;
      }
      return (
        <div className="box-action phn">
          <div className="container container-c">
            <div className="columns">
              <div className="column column-page-sidebar">
                <h3 className="mtn">
                  {ifPresent(this.props.groupData.icon, (icon) => (
                    <span className="mrm type-icon">{icon}</span>
                  ))}
                  <span>{this.getName()}</span>
                </h3>

                {this.renderMetaInfo()}
              </div>
              <div className="column column-page-main">
                {this.props.groupData && this.props.groupData.description ? (
                  <p className="mbl">{this.props.groupData.description}</p>
                ) : null}

                {this.renderBehaviors()}
                <div className="mvxl">
                  {this.renderPrimaryAction()}
                  <button type="button" className="mrs mbs" onClick={this.toggle}>Done</button>
                </div>
              </div>
            </div>
          </div>
        </div>
      );
    }

    renderBehaviors(): React.Node {
      const behaviors = this.getBehaviors();
      const behaviorCount = behaviors.length;
      const exportId = this.props.groupData ? this.props.groupData.exportId : "";
      const hasGroupId = Boolean(this.props.groupData && this.props.groupData.id);
      return (
        <div className="type-s">
          <h5 className="mtn mbxs">{behaviorCount === 1 ? "1 action" : `${behaviorCount} actions`}</h5>
          <div style={{ overflowY: "auto", maxHeight: "21em" }}>
            {behaviors.map((behavior, index) => (
              <div className="pvs" key={`group-${exportId}-behavior${index}`}>
                <EditableName
                  version={behavior}
                  disableLink={!hasGroupId || !behavior.behaviorId}
                  isImportable={true}
                />
              </div>
            ))}
          </div>
        </div>
      );
    }

    renderPrimaryAction(): React.Node {
      if (this.props.localId) {
        return (
          <a href={jsRoutes.controllers.BehaviorEditorController.edit(this.props.localId).url} className="button mrs mbs">
            Edit skill…
          </a>
        );
      } else if (this.props.isImportable && !this.props.localId) {
        return (
          <button type="button" className="button-primary mrs mbs" onClick={this.onImport}>
            <span className="display-inline-block align-b mrm pbxs"
              style={{ width: 25, height: 18 }}><SVGInstall /></span>
            <span className="display-inline-block align-b">Install</span>
          </button>
        );
      }
    }

    renderUpdate(): React.Node {
      if (this.props.updatedData) {
        return (
          <div className="mvl fade-in">
            <button type="button" className="button-s" onClick={this.onUpdate}>Re-install</button>
          </div>
        );
      } else {
        return null;
      }
    }

    renderInstallInfo(): React.Node {
      if (this.props.groupData && this.props.groupData.githubUrl) {
        return (
          <div className="type-s mvm">
            <a target="_blank" href={this.props.groupData.githubUrl}>
              View source on Github
            </a>
          </div>
        );
      } else if (this.props.wasImported) {
        return (
          <div className="type-s mvm fade-in">
            <span className="display-inline-block align-m mrs" style={{ width: 30, height: 18 }}><SVGInstalled /></span>
            <span className="display-inline-block align-m type-green">Installed from Ellipsis.ai</span>
          </div>
        );
      }
    }

    renderMetaInfo(): React.Node {
      return (
        <div>
          {this.renderLastModified()}
          {this.renderInstallInfo()}
          {this.renderUpdate()}
        </div>
      );
    }

    renderLastModified(): React.Node {
      if (this.props.localId && this.props.groupData && this.props.groupData.createdAt) {
        return (
          <div className="type-weak type-s mvm">
            Last modified {Formatter.formatTimestampRelativeIfRecent(this.props.groupData.createdAt)}
          </div>
        );
      }
    }
  }

export default BehaviorGroupInfoPanel;
