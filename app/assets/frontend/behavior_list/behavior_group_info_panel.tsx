import * as React from 'react';
import BehaviorGroup from '../models/behavior_group';
import BehaviorVersion from '../models/behavior_version';
import DynamicLabelButton from '../form/dynamic_label_button';
import EditableName from './editable_name';
import Formatter, {Timestamp} from '../lib/formatter';
import SVGInstall from '../svg/install';
import Sort from '../lib/sort';
import autobind from '../lib/autobind';
import User from "../models/user";

  type Props = {
    groupData: Option<BehaviorGroup>,
    onBehaviorGroupImport: (BehaviorGroup) => void,
    onBehaviorGroupUpdate: (originalGroup: BehaviorGroup, updatedGroup: BehaviorGroup) => void,
    onToggle: () => void,
    isImportable: boolean,
    publishedGroupData: Option<BehaviorGroup>,
    publishedGroupDiffers: boolean,
    isImporting: boolean,
    localId: Option<string>
  }

  class BehaviorGroupInfoPanel extends React.PureComponent<Props> {
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

    onRevertToPublished(): void {
      if (this.props.groupData && this.props.publishedGroupData) {
        this.props.onBehaviorGroupUpdate(this.props.groupData, this.props.publishedGroupData);
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
      const alsoPublished = this.props.publishedGroupData;
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

                {this.renderMetaInfo(group, alsoPublished, this.props.publishedGroupDiffers)}
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

    renderUpdate(publishedData: Option<BehaviorGroup>, differsFromPublished: boolean) {
      if (publishedData && this.props.localId) {
        return (
          <div className="mvl fade-in">
            <DynamicLabelButton
              className="button-s"
              disabledWhen={this.props.isImporting || !differsFromPublished}
              onClick={this.onRevertToPublished}
              labels={[{
                text: "Revert to published version",
                displayWhen: !this.props.isImporting && differsFromPublished
              }, {
                text: "Synced with published version",
                displayWhen: !this.props.isImporting && !differsFromPublished
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

    renderManagedContact(contact?: Option<User>) {
      if (contact) {
        return (
          <div>
            <span className="type-weak">Contact: </span>
            <span>{contact.formattedNameIfKnown()}</span>
          </div>
        );
      } else {
        return null;
      }
    }

    renderGithubLink(url: Option<string>) {
      return url ? (
        <span className="mlxs">
          <span className="display-nowrap">
            <span className="type-disabled mrxs">·</span>
            <a target="_blank" href={url}>View source</a>
          </span>
        </span>
      ) : null;
    }

    renderSourceInfo(group: BehaviorGroup) {
      const groupGithubUrl = group.linkedGithubRepo ? group.linkedGithubRepo.getUrl() : null;
      if (group.isManaged) {
        return (
          <div className="type-s mvm">
            <div>
              {this.props.publishedGroupData ? (
                <b className="type-pink">Published/managed by Ellipsis</b>
              ) : (
                <b className="type-pink">Managed by Ellipsis</b>
              )}
              {this.renderGithubLink(groupGithubUrl)}
            </div>
            {this.renderManagedContact(group.managedContact)}
          </div>
        )
      } else if (this.props.publishedGroupData) {
        return (
          <div className={`type-s mvm ${this.props.isImporting ? "pulse" : ""}`}>
            <span className="type-green">Published by Ellipsis</span>
            {this.renderGithubLink(groupGithubUrl)}
          </div>
        );
      } else {
        return (
          <div className="type-s mvm">
            <span>Custom skill</span>
            {this.renderGithubLink(groupGithubUrl)}
          </div>
        );
      }
    }

    renderMetaInfo(group: BehaviorGroup, publishedData: Option<BehaviorGroup>, differsFromPublished: boolean) {
      return (
        <div>
          {this.renderSourceInfo(group)}
          {this.renderHistory(group, Boolean(publishedData), differsFromPublished)}
          {this.renderUpdate(publishedData, differsFromPublished)}
        </div>
      );
    }

    renderInitialCreatedText(dateText: Option<string>, initialAuthor: string, isPublished: boolean) {
      if (dateText) {
        if (isPublished) {
          return (
            <li>Installed {dateText} by {initialAuthor}</li>
          );
        } else {
          return (
            <li>Created {dateText} by {initialAuthor}</li>
          );
        }
      } else {
        return null;
      }
    }

    renderLastModifiedText(lastModified: Timestamp, group: BehaviorGroup, isPublished: boolean, differsFromPublished: boolean) {
      const DEFAULT_NAME = "an unknown user";

      const initialCreated = group.getInitialCreatedAt();
      let initialCreatedDate = initialCreated ? Formatter.formatTimestampRelativeIfRecent(initialCreated) : null;
      let lastModifiedDate = Formatter.formatTimestampRelativeIfRecent(lastModified);
      const hasChangedSinceCreation = lastModified !== initialCreated;

      // Make the date text more precise if the short versions appear the same when the underlying timestamps are different
      if (hasChangedSinceCreation && initialCreated && initialCreatedDate === lastModifiedDate) {
        initialCreatedDate = Formatter.formatTimestampShort(initialCreated);
        lastModifiedDate = Formatter.formatTimestampShort(lastModified);
      }

      const initialAuthor = group.getInitialAuthor();
      const initialAuthorName = initialAuthor ? initialAuthor.formattedFullNameOrUserName(DEFAULT_NAME) : DEFAULT_NAME;
      const currentAuthorName = group.author ? group.author.formattedFullNameOrUserName(DEFAULT_NAME) : DEFAULT_NAME;

      if (isPublished && differsFromPublished) {
        return (
          <ul>
            {this.renderInitialCreatedText(initialCreatedDate, initialAuthorName, isPublished)}
            <li>Last modified {lastModifiedDate} by {currentAuthorName}</li>
            <li>This skill differs from published version</li>
          </ul>
        );
      } else if (isPublished) {
        return (
          <ul>
            {this.renderInitialCreatedText(initialCreatedDate, initialAuthorName, isPublished)}
            {hasChangedSinceCreation ? (
              <li>Re-installed {lastModifiedDate} by {currentAuthorName}</li>
            ) : null}
          </ul>
        );
      } else {
        return (
          <ul>
            {this.renderInitialCreatedText(initialCreatedDate, initialAuthorName, isPublished)}
            {hasChangedSinceCreation ? (
              <li>Last modified {lastModifiedDate} by {currentAuthorName}</li>
            ) : null}
          </ul>
        );
      }
    }

    renderHistory(group: BehaviorGroup, isPublished: boolean, differsFromPublished: boolean) {
      if (this.props.localId && group.createdAt) {
        return (
          <div className="type-weak type-s mvm">
            {this.renderLastModifiedText(group.createdAt, group, isPublished, differsFromPublished)}
          </div>
        );
      } else {
        return null;
      }
    }
  }

export default BehaviorGroupInfoPanel;
