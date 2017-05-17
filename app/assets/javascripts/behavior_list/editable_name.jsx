define(function(require) {
  var React = require('react'),
    Editable = require('../models/editable'),
    SubstringHighlighter = require('../shared_ui/substring_highlighter'),
    ImmutableObjectUtils = require('../lib/immutable_object_utils');

  return React.createClass({
    displayName: 'EditableName',
    propTypes: {
      version: React.PropTypes.instanceOf(Editable).isRequired,
      disableLink: React.PropTypes.bool,
      omitDescription: React.PropTypes.bool,
      labelDataType: React.PropTypes.bool,
      onClick: React.PropTypes.func,
      isImportable: React.PropTypes.bool,
      className: React.PropTypes.string,
      triggerClassName: React.PropTypes.string,
      highlightText: React.PropTypes.string
    },

    getTriggerClass: function() {
      return "box-chat " + (this.props.triggerClassName || "");
    },

    getLabelFromTrigger: function(trigger, showLink) {
      var className = showLink ? "link" : "";
      if (trigger && trigger.text) {
        return (
          <span className={`${className} ${this.getTriggerClass()} mrs`}>
            <SubstringHighlighter text={trigger.displayText} substring={this.props.highlightText} />
          </span>
        );
      } else if (this.props.version.isNew && !this.props.isImportable) {
        return (
          <span className={`${className} type-italic mrs`}>New action</span>
        );
      } else if (!this.props.version.name && !this.props.version.description) {
        return (
          <span className={`${className} type-italic mrs`}>(No triggers)</span>
        );
      }
    },

    getNonRegexTriggerLabelsFromTriggers: function(triggers) {
      return triggers.filter((trigger) => !trigger.isRegex).map((trigger, index) => {
        if (trigger.text) {
          return (
            <span className={`${this.getTriggerClass()} mrs`} key={"regularTrigger" + index}>
              <SubstringHighlighter text={trigger.displayText} substring={this.props.highlightText} />
            </span>
          );
        } else {
          return null;
        }
      });
    },

    getNonRegexTriggerText: function(triggers) {
      return triggers.filter((trigger) => !trigger.isRegex).map((ea) => ea.displayText).filter((ea) => !!ea.trim()).join("\n");
    },

    getRegexTriggerText: function(triggers) {
      var regexTriggerCount = triggers.filter((trigger) => !!trigger.isRegex).length;

      if (regexTriggerCount === 1) {
        return "also matches another pattern";
      } else if (regexTriggerCount > 1) {
        return `also matches ${regexTriggerCount} other patterns`;
      } else {
        return "";
      }
    },

    getRegexTriggerLabelFromTriggers: function(triggers) {
      var text = this.getRegexTriggerText(triggers);

      if (text) {
        return (
          <span className="mrs type-italic">({text})</span>
        );
      } else {
        return null;
      }
    },

    getBehaviorSummary: function(firstTrigger, otherTriggers) {
      var name = this.props.version.name;
      var description = this.props.version.description;
      var firstTriggerText = firstTrigger ? firstTrigger.displayText : "(None)";
      var nonRegex = this.getNonRegexTriggerText(otherTriggers);
      var regex = this.getRegexTriggerText(otherTriggers);
      return (name ? `Name: ${name}\n\n` : "") +
        (description ? `Description: ${description}\n\n` : "") +
        "Triggers:\n" +
        firstTriggerText +
        (nonRegex ? "\n" + nonRegex : "") +
        (regex ? `\n(${regex})` : "");
    },

    getTriggersFromVersion: function(version, linkFirstTrigger) {
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
    },

    getDisplayClassesFor: function(version) {
      const italic = !version.name ? "type-italic" : "";
      return `${italic} display-limit-width display-ellipsis`;
    },

    getDataTypeLabelFromVersion: function(version) {
      return (
        <div className={this.getDisplayClassesFor(version)}>
          <span className={this.props.disableLink ? "" : "link"}>{version.name || "New data type"}</span>
          {this.props.labelDataType ? (
            <span> (data type)</span>
          ) : null}
        </div>
      );
    },

    geLibraryLabelFromVersion: function(version) {
      return (
        <div className={this.getDisplayClassesFor(version)}>
          <span className={this.props.disableLink ? "" : "link"}>{version.name || "New library"}</span>
        </div>
      );
    },

    getActionLabelFromVersion: function(version) {
      const name = version.name;
      const includeDescription = version.description && !this.props.omitDescription;
      if (name && name.trim().length > 0) {
        return (
          <div>
            <div className="display-limit-width display-ellipsis">
              <span className={"mrs " + (this.props.disableLink ? "" : "link")}>
                <SubstringHighlighter text={name} substring={this.props.highlightText} />
              </span>
              {this.getTriggersFromVersion(version, false)}
            </div>
            {includeDescription ? (
                <div className="display-limit-width display-ellipsis">
                  {this.getDescriptionFromVersion(this.props.version)}
                </div>
              ) : null}
          </div>
        );
      } else {
        return (
          <div className="display-limit-width display-ellipsis">
            {this.getTriggersFromVersion(version, !this.props.disableLink)}
          </div>
        );
      }
    },

    getLabelFromVersion: function(version) {
      if (version.isBehaviorVersion()) {
        if (version.isDataType()) {
          return this.getDataTypeLabelFromVersion(version);
        } else {
          return this.getActionLabelFromVersion(version);
        }
      } else {
        return this.geLibraryLabelFromVersion(version);
      }
    },

    getDescriptionFromVersion: function(version) {
      if (!this.props.omitDescription && version.description) {
        return (
          <span className="type-italic type-weak pbxs">
            <SubstringHighlighter text={version.description} substring={this.props.highlightText} />
          </span>
        );
      }
    },

    onLinkClick: function(event) {
      if (this.props.onClick) {
        this.props.onClick(this.props.version.groupId, this.props.version.getPersistentId());
        event.preventDefault();
      }
    },

    render: function() {
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
              href={jsRoutes.controllers.BehaviorEditorController.edit(this.props.version.groupId, this.props.version.getPersistentId()).url}
              onClick={this.onLinkClick}
              className={"link-block " + (this.props.className || "")}>
              {this.getLabelFromVersion(this.props.version)}
            </a>
          </div>
        );
      }
    }
  });
});
