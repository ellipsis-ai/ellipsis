import * as React from 'react';
import Editable from '../models/editable';
import SubstringHighlighter from '../shared_ui/substring_highlighter';
import ImmutableObjectUtils from '../lib/immutable_object_utils';
import Trigger from "../models/trigger";
import BehaviorVersion from "../models/behavior_version";
import autobind from "../lib/autobind";
import {ReactNode} from "react";

interface Props {
  version: Editable,
  disableLink?: Option<boolean>,
  omitDescription?: Option<boolean>,
  labelDataType?: Option<boolean>,
  onClick?: Option<(groupId: string, behaviorId: string) => void>,
  isImportable?: Option<boolean>,
  className?: Option<string>,
  triggerClassName?: Option<string>,
  highlightText?: Option<string>,
  renderStatus?: (Editable) => ReactNode
}

class EditableName extends React.Component<Props> {
    constructor(props: Props) {
      super(props);
      autobind(this);
    }

    getTriggerClass(): string {
      return "box-chat " + (this.props.triggerClassName || "");
    }

    getLabelFromTrigger(trigger: Trigger, showLink: boolean) {
      var className = showLink ? "link" : "";
      if (trigger && trigger.text) {
        return (
          <span className={`${className} ${this.getTriggerClass()} mrs`}>
            <SubstringHighlighter text={trigger.displayText()} substring={this.props.highlightText} />
          </span>
        );
      } else if (this.props.version.isNew && !this.props.isImportable) {
        return (
          <span className={`${className} type-italic mrs`}>New action</span>
        );
      } else if (!this.props.version.name && !this.props.version.description) {
        return (
          <span className={`${className} type-italic mrs`}>Unnamed action</span>
        );
      } else {
        return null;
      }
    }

    getNonRegexTriggerLabelsFromTriggers(triggers: Array<Trigger>) {
      return triggers.filter((trigger) => !trigger.isRegex).map((trigger, index) => {
        if (trigger.text) {
          return (
            <span className={`${this.getTriggerClass()} mrs`} key={"regularTrigger" + index}>
              <SubstringHighlighter text={trigger.displayText()} substring={this.props.highlightText} />
            </span>
          );
        } else {
          return null;
        }
      });
    }

    getNonRegexTriggerText(triggers: Array<Trigger>): string {
      return triggers.filter((trigger) => !trigger.isRegex).map((ea) => ea.displayText()).filter((ea) => !!ea.trim()).join("\n");
    }

    getRegexTriggerText(triggers: Array<Trigger>): string {
      var regexTriggerCount = triggers.filter((trigger) => !!trigger.isRegex).length;

      if (regexTriggerCount === 1) {
        return "also matches another pattern";
      } else if (regexTriggerCount > 1) {
        return `also matches ${regexTriggerCount} other patterns`;
      } else {
        return "";
      }
    }

    getRegexTriggerLabelFromTriggers(triggers: Array<Trigger>) {
      var text = this.getRegexTriggerText(triggers);

      if (text) {
        return (
          <span className="mrs type-italic">({text})</span>
        );
      } else {
        return null;
      }
    }

    getBehaviorSummary(firstTrigger: Option<Trigger>, otherTriggers: Array<Trigger>): string {
      var name = this.props.version.name;
      var description = this.props.version.description;
      var firstTriggerText = firstTrigger ? firstTrigger.displayText() : "(None)";
      var nonRegex = this.getNonRegexTriggerText(otherTriggers);
      var regex = this.getRegexTriggerText(otherTriggers);
      return (name ? `Name: ${name}\n\n` : "") +
        (description ? `Description: ${description}\n\n` : "") +
        "Triggers:\n" +
        firstTriggerText +
        (nonRegex ? "\n" + nonRegex : "") +
        (regex ? `\n(${regex})` : "");
    }

    getTriggersFromVersion(version: BehaviorVersion, linkFirstTrigger: boolean) {
      var firstTriggerIndex = version.findFirstTriggerIndexForDisplay();
      var firstTrigger = version.triggers[firstTriggerIndex];
      var otherTriggers = ImmutableObjectUtils.arrayRemoveElementAtIndex(version.triggers, firstTriggerIndex);
      return (
        <span title={this.getBehaviorSummary(firstTrigger, otherTriggers)}>
          {this.getLabelFromTrigger(firstTrigger, linkFirstTrigger)}
          {this.getNonRegexTriggerLabelsFromTriggers(otherTriggers)}
          {this.getRegexTriggerLabelFromTriggers(otherTriggers)}
        </span>
      );
    }

    getDisplayClassesFor(version: Editable): string {
      const italic = !version.name ? "type-italic" : "";
      return `${italic} display-limit-width display-ellipsis`;
    }

    getDataTypeLabelFromVersion(version: BehaviorVersion) {
      return (
        <div className={this.getDisplayClassesFor(version)}>
          <span className={this.props.disableLink ? "" : "link"}>{version.name || "New data type"}</span>
          {this.props.labelDataType ? (
            <span>{" (data type)"}</span>
          ) : null}
        </div>
      );
    }

    getLibraryLabelFromVersion(version: Editable) {
      return (
        <div className={this.getDisplayClassesFor(version)}>
          <span className={this.props.disableLink ? "" : "link"}>{version.name || "New library"}</span>
        </div>
      );
    }

    getActionLabelFromVersion(version: BehaviorVersion) {
      const name = version.name;
      const includeDescription = version.description && !this.props.omitDescription;
      if (name && name.trim().length > 0) {
        return (
          <div>
            <div className="display-limit-width display-ellipsis">
              {this.renderStatus()}
              <span className={"mrs " + (this.props.disableLink ? "" : "link")}>
                <SubstringHighlighter text={name} substring={this.props.highlightText} />
              </span>
              {this.getTriggersFromVersion(version, false)}
            </div>
            {includeDescription ? (
                <div className="display-limit-width display-ellipsis">
                  {this.getDescriptionFromVersion(version)}
                </div>
              ) : null}
          </div>
        );
      } else {
        return (
          <div className="display-limit-width display-ellipsis">
            {this.renderStatus()}
            {this.getTriggersFromVersion(version, !this.props.disableLink)}
          </div>
        );
      }
    }

    getLabelFromVersion(version: Editable) {
      if (version.isBehaviorVersion()) {
        if (version.isDataType()) {
          return this.getDataTypeLabelFromVersion(version);
        } else {
          return this.getActionLabelFromVersion(version);
        }
      } else {
        return this.getLibraryLabelFromVersion(version);
      }
    }

    getDescriptionFromVersion(version: BehaviorVersion) {
      if (!this.props.omitDescription && version.description) {
        return (
          <span className="type-italic type-weak pbxs">
            <SubstringHighlighter text={version.description} substring={this.props.highlightText} />
          </span>
        );
      } else {
        return null;
      }
    }

    onLinkClick(event: React.MouseEvent<HTMLAnchorElement>) {
      if (this.props.onClick && this.props.version.groupId) {
        this.props.onClick(this.props.version.groupId, this.props.version.getPersistentId());
        event.preventDefault();
      }
    }

    renderStatus(): ReactNode {
      if (this.props.renderStatus) {
        return this.props.renderStatus(this.props.version);
      } else {
        return null;
      }
    }

    render() {
      if (this.props.disableLink) {
        return (
          <div className={this.props.className || ""}>
            {this.getLabelFromVersion(this.props.version)}
          </div>
        );
      } else if (this.props.version.groupId) {
        return (
          <div>
            <a
              href={jsRoutes.controllers.BehaviorEditorController.edit(this.props.version.groupId, this.props.version.getPersistentId()).url}
              onClick={this.onLinkClick}
              className={"link-block " + (this.props.className || "")}>
              {this.getLabelFromVersion(this.props.version)}
            </a>
          </div>
        );
      } else {
        return null;
      }
    }
}

export default EditableName;
