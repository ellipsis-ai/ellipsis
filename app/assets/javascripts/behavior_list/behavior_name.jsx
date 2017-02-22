define(function(require) {
  var React = require('react'),
    BehaviorVersion = require('../models/behavior_version'),
    ImmutableObjectUtils = require('../lib/immutable_object_utils');

  return React.createClass({
    displayName: 'BehaviorName',
    propTypes: {
      version: React.PropTypes.instanceOf(BehaviorVersion).isRequired,
      disableLink: React.PropTypes.bool,
      omitDescription: React.PropTypes.bool,
      limitTriggers: React.PropTypes.bool,
      labelDataType: React.PropTypes.bool
    },

    getLabelFromTrigger: function(trigger, showLink) {
      var className = showLink ? "link" : "";
      return trigger && trigger.text ?
        (<span className={`${className} type-monospace`}>{trigger.displayText}</span>) :
        (<span className={`${className} type-italic`}>(No triggers)</span>);
    },

    getNonRegexTriggerLabelsFromTriggers: function(triggers) {
      return triggers.filter((trigger) => !trigger.isRegex).map((trigger, index) => {
        if (trigger.text) {
          return (
            <span className="type-monospace" key={"regularTrigger" + index}>
              <span className="type-disabled"> · </span>
              <span>{trigger.displayText}</span>
            </span>
          );
        } else {
          return null;
        }
      });
    },

    getRegexTriggerLabelFromTriggers: function(triggers) {
      var regexTriggerCount = triggers.filter((trigger) => !!trigger.isRegex).length;

      var text = regexTriggerCount === 1 ?
        "also matches another pattern" :
        `also matches ${regexTriggerCount} other patterns`;

      if (regexTriggerCount > 0) {
        return (
          <span>
            <span className="type-monospace type-disabled"> · </span>
            <span className="type-italic">{text}</span>
          </span>
        );
      } else {
        return null;
      }
    },

    getTriggersFromVersion: function(version, linkFirstTrigger) {
      var firstTriggerIndex = version.findFirstTriggerIndexForDisplay();
      var firstTrigger = version.triggers[firstTriggerIndex];
      var otherTriggers = ImmutableObjectUtils.arrayRemoveElementAtIndex(version.triggers, firstTriggerIndex);
      return (
        <span>
          {this.getLabelFromTrigger(firstTrigger, linkFirstTrigger)}
          {this.props.limitTriggers ? null : (
            <span>
              {this.getNonRegexTriggerLabelsFromTriggers(otherTriggers)}
              {this.getRegexTriggerLabelFromTriggers(otherTriggers)}
            </span>
          )}
        </span>
      );
    },

    getDataTypeLabelFromVersion: function(version) {
      return (
        <div className={"type-italic " + (this.props.limitTriggers ? "display-ellipsis" : "")}>
          <span className="link">{version.getDataTypeName()}</span>
          {this.props.labelDataType ? (
            <span className="type-weak"> (data type)</span>
          ) : null}
        </div>
      );
    },

    getActionLabelFromVersion: function(version) {
      const name = version.name;
      if (name && name.trim().length > 0) {
        return (
          <div className={(this.props.limitTriggers ? "display-ellipsis" : "")}>
            <span className={"mrm " + (this.props.disableLink ? "" : "link")}>{name}:</span>
            {this.getTriggersFromVersion(version, false)}
          </div>
        );
      } else {
        return this.getTriggersFromVersion(version, !this.props.disableLink);
      }
    },

    getLabelFromVersion: function(version) {
      if (version.isDataType()) {
        return this.getDataTypeLabelFromVersion(version);
      } else {
        return this.getActionLabelFromVersion(version);
      }
    },

    getDescriptionFromVersion: function(version) {
      if (!this.props.omitDescription && version.description) {
        return this.props.limitTriggers ? (
          <span className="type-italic type-weak pbxs"> &nbsp;·&nbsp; {version.description}</span>
        ) : (
          <div className="type-italic type-weak pbxs ">{version.description}</div>
        );
      }
    },

    render: function() {
      if (this.props.disableLink) {
        return (
          <div className={this.props.limitTriggers ? "display-ellipsis" : ""}>
            {this.getLabelFromVersion(this.props.version)}
            {this.getDescriptionFromVersion(this.props.version)}
          </div>
        );
      } else {
        return (
          <div className={this.props.limitTriggers ? "display-ellipsis" : ""}>
            <a href={jsRoutes.controllers.BehaviorEditorController.edit(this.props.version.behaviorId).url}
              className="link-block">
              {this.getLabelFromVersion(this.props.version)}
              {this.getDescriptionFromVersion(this.props.version)}
            </a>
          </div>
        );
      }
    }
  });
});
