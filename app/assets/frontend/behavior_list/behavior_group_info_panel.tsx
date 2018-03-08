import * as React from 'react';
import BehaviorGroup from '../models/behavior_group';
import BehaviorVersion from '../models/behavior_version';
import DynamicLabelButton from '../form/dynamic_label_button';
import EditableName from './editable_name';
import Formatter, {Timestamp} from '../lib/formatter';
import SVGInstall from '../svg/install';
import SVGInstalled from '../svg/installed';
import ifPresent from '../lib/if_present';
import Sort from '../lib/sort';
import autobind from '../lib/autobind';
import {maybeDiffFor} from "../models/diffs";

  type Props = {
    groupData: BehaviorGroup | null,
    onBehaviorGroupImport: (BehaviorGroup) => void,
    onBehaviorGroupUpdate: (originalGroup: BehaviorGroup, updatedGroup: BehaviorGroup) => void,
    updatedData: BehaviorGroup | null,
    onToggle: () => void,
    isImportable: boolean,
    publishedGroupData: BehaviorGroup | null,
    isImporting: boolean,
    localId: string | null
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

    getName(group: BehaviorGroup) {
      return group.name || (
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

    render() {
      const group = this.props.groupData;
      if (!group) {
        return null;
      }
      const icon = group.icon;
      return (
        <div className="box-action phn">
          <div className="container container-c">
            <div className="columns">
              <div className="column column-page-sidebar">
                <h3 className="mtn">
                  {icon ? (
                    <span className="mrm type-icon">{icon}</span>
                  ) : null}
                  <span>{this.getName(group)}</span>
                </h3>

                {this.renderMetaInfo(group)}
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

    renderBehaviors() {
      const behaviors = this.getBehaviors();
      const behaviorCount = behaviors.length;
      const exportId = this.props.groupData && this.props.groupData.exportId ? this.props.groupData.exportId : "";
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

    renderPrimaryAction() {
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
      } else {
        return null;
      }
    }

    renderUpdate() {
      if (this.props.updatedData && this.props.localId) {
        return (
          <div className="mvl fade-in">
            <DynamicLabelButton
              className="button-s"
              disabledWhen={this.props.isImporting}
              onClick={this.onUpdate}
              labels={[{
                text: "Revert to published version",
                displayWhen: !this.props.isImporting
              }, {
                text: "Re-installing",
                displayWhen: this.props.isImporting
              }]}
            />
          </div>
        );
      } else {
        return null;
      }
    }

    renderInstallInfo(group: BehaviorGroup) {
      const groupGithubUrl = group.githubUrl;
      if (groupGithubUrl) {
        return (
          <div className="type-s mvm">
            <a target="_blank" href={groupGithubUrl}>
              View source on Github
            </a>
          </div>
        );
      } else if (this.props.groupData && this.props.publishedGroupData) {
        return (
          <div className={`type-s mvm fade-in ${this.props.isImporting ? "pulse" : ""}`}>
            <span className="display-inline-block align-m mrs" style={{ width: 30, height: 18 }}><SVGInstalled /></span>
            <span className="display-inline-block align-m type-green">Skill published by Ellipsis</span>
          </div>
        );
      } else {
        return null;
      }
    }

    renderMetaInfo(group: BehaviorGroup) {
      return (
        <div>
          {this.renderInstallInfo(group)}
          {this.renderLastModified(group)}
          {this.renderUpdate()}
        </div>
      );
    }

    renderLastModifiedText(group: BehaviorGroup, createdAt: Timestamp) {
      const lastModifiedDate = Formatter.formatTimestampRelativeIfRecent(createdAt);
      const authorName = group.author ? group.author.formattedFullNameOrUserName() : "an unknown user";
      const alsoPublished = this.props.publishedGroupData;
      const maybeDiff = alsoPublished && maybeDiffFor(group, alsoPublished);
      const sameAsPublished = alsoPublished && !maybeDiff;
      if (sameAsPublished) {
        return (
          <span>Installed {lastModifiedDate} by {authorName}</span>
        );
      } else if (alsoPublished) {
        return (
          <span>Modified since it was installed. Last modified {lastModifiedDate} by {authorName}</span>
        );
      } else {
        return (
          <span>Last modified {lastModifiedDate} by {authorName}</span>
        );
      }
    }

    renderLastModified(group: BehaviorGroup) {
      if (this.props.localId && group.createdAt) {
        return (
          <div className="type-weak type-s mvm">
            {this.renderLastModifiedText(group, group.createdAt)}
          </div>
        );
      } else {
        return null;
      }
    }
  }

export default BehaviorGroupInfoPanel;
