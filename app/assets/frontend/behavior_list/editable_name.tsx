import * as React from 'react';
import Editable from '../models/editable';
import SubstringHighlighter from '../shared_ui/substring_highlighter';
import ImmutableObjectUtils from '../lib/immutable_object_utils';
import Trigger from "../models/trigger";
import BehaviorVersion from "../models/behavior_version";
import autobind from "../lib/autobind";
import {ReactNode} from "react";
import {BehaviorSelectCallback} from "../behavior_editor/behavior_switcher";
import {Emoji} from "emoji-mart";

const EMOJI_SET = "twitter";
const EMOJI_SHEET_SIZE = 32;
const EMOJI_SIZE = 14;

interface Props {
  version: Editable,
  disableLink?: Option<boolean>,
  omitDescription?: Option<boolean>,
  labelType?: Option<boolean>,
  onClick?: Option<BehaviorSelectCallback>,
  isImportable?: Option<boolean>,
  className?: Option<string>,
  highlightText?: Option<string>,
  renderStatus?: (e: Editable) => ReactNode
  enableWrapping?: Option<boolean>
  selected?: Option<boolean>
}

class EditableName extends React.Component<Props> {
    constructor(props: Props) {
      super(props);
      autobind(this);
    }

    getTriggerClass(): string {
      return `box-chat ${this.props.selected ? "box-chat-selected" : ""}`;
    }

    renderEmojiTrigger(trigger: Trigger, key?: string) {
      return (
        <span
          className={`box-emoji-reaction ${this.props.selected ? "box-emoji-reaction-selected" : ""} mrs`}
          title={`When user reacts with the :${trigger.text}: emoji`}
          key={key}
        >
          <span className="display-inline-block opacity-75 mrxs">+</span>
          <span className="display-inline-block align-t mtneg1">
            <Emoji
              native={true}
              emoji={trigger.text}
              size={EMOJI_SIZE}
              sheetSize={EMOJI_SHEET_SIZE}
              set={EMOJI_SET}
            />
          </span>
        </span>
      );
    }

    getLabelFromTrigger(trigger: Option<Trigger>, showLink: boolean) {
      var className = showLink ? "link" : "";
      if (trigger && trigger.isReactionAddedTrigger()) {
        return this.renderEmojiTrigger(trigger);
      } else if (trigger && trigger.text) {
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
      return triggers.filter((trigger) => !trigger.isRegex).sort((a, b) => {
        if (a.isReactionAddedTrigger() && !b.isReactionAddedTrigger()) {
          return 1;
        } else if (b.isReactionAddedTrigger() && !a.isReactionAddedTrigger()) {
          return -1;
        } else {
          return 0;
        }
      }).map((trigger, index) => {
        const key = "regularTrigger" + index;
        if (trigger.isReactionAddedTrigger()) {
          return this.renderEmojiTrigger(trigger, key);
        } else if (trigger.text) {
          return (
            <span className={`${this.getTriggerClass()} mrs`} key={key}>
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
      var firstTrigger: Option<Trigger> = version.triggers[firstTriggerIndex];
      var otherTriggers = ImmutableObjectUtils.arrayRemoveElementAtIndex(version.triggers, firstTriggerIndex);
      return (
        <span title={this.getBehaviorSummary(firstTrigger, otherTriggers)}>
          {this.getLabelFromTrigger(firstTrigger, linkFirstTrigger)}
          {this.getNonRegexTriggerLabelsFromTriggers(otherTriggers)}
          {this.getRegexTriggerLabelFromTriggers(otherTriggers)}
        </span>
      );
    }

    getWrapClasses(): string {
      return this.props.enableWrapping ? "" : "display-limit-width display-ellipsis";
    }

    getDisplayClassesFor(version: Editable): string {
      const italic = !version.name ? "type-italic" : "";
      return `${italic} ${this.getWrapClasses()}`;
    }

    getDataTypeLabelFromVersion(version: BehaviorVersion) {
      return (
        <div className={this.getDisplayClassesFor(version)}>
          <span className={this.props.disableLink ? "" : "link"}>{version.name || "New data type"}</span>
          {this.props.labelType ? (
            <span>{" (data type)"}</span>
          ) : null}
        </div>
      );
    }

    getLibraryLabelFromVersion(version: Editable) {
      return (
        <div className={this.getDisplayClassesFor(version)}>
          <span className={this.props.disableLink ? "" : "link"}>{version.name || "New library"}</span>
          {this.props.labelType ? (
            <span>{" (library)"}</span>
          ) : null}
        </div>
      );
    }

    getActionLabelFromVersion(version: BehaviorVersion) {
      const name = version.name;
      const includeDescription = version.description && !this.props.omitDescription;
      if (name && name.trim().length > 0) {
        return (
          <div>
            <div className={this.getWrapClasses()}>
              {this.renderStatus()}
              <span className={"mrs " + (this.props.disableLink ? "" : "link")}>
                <SubstringHighlighter text={name} substring={this.props.highlightText} />
              </span>
              {this.getTriggersFromVersion(version, false)}
            </div>
            {includeDescription ? (
                <div className={this.getWrapClasses()}>
                  {this.getDescriptionFromVersion(version)}
                </div>
              ) : null}
          </div>
        );
      } else {
        return (
          <div className={this.getWrapClasses()}>
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
      if (this.props.onClick) {
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
      const teamId = this.props.version.teamId;
      const behaviorGroupId = this.props.version.groupId;
      let link: string;
      if (behaviorGroupId) {
        link = jsRoutes.controllers.BehaviorEditorController.edit(behaviorGroupId, this.props.version.getPersistentId()).url;
      } else if (teamId) {
        link = jsRoutes.controllers.BehaviorEditorController.newGroup(teamId).url;
      } else {
        link = window.location.href;
      }
      if (this.props.disableLink) {
        return (
          <div className={this.props.className || ""}>
            {this.getLabelFromVersion(this.props.version)}
          </div>
        );
      } else {
        return (
          <div>
            <a
              href={link}
              onClick={this.onLinkClick}
              className={"link-block " + (this.props.className || "")}>
              {this.getLabelFromVersion(this.props.version)}
            </a>
          </div>
        );
      }
    }
}

export default EditableName;
