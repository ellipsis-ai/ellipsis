define(function(require) {
  var React = require('react'),
    BehaviorVersion = require('../models/behavior_version'),
    ImmutableObjectUtils = require('../immutable_object_utils');

  return React.createClass({
    displayName: 'BehaviorName',
    propTypes: {
      version: React.PropTypes.instanceOf(BehaviorVersion).isRequired,
      disableLink: React.PropTypes.bool,
      disableWrapping: React.PropTypes.bool,
      labelDataType: React.PropTypes.bool
    },

    getLabelFromTrigger: function(trigger) {
      var className = this.props.disableLink ? "" : "link";
      return trigger && trigger.text ?
        (<span className={`${className} type-monospace`}>{trigger.displayText}</span>) :
        (<span className={`${className} type-italic`}>(New skill)</span>);
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

    getTriggersFromVersion: function(version) {
      var firstTriggerIndex = version.findFirstTriggerIndexForDisplay();
      var firstTrigger = version.triggers[firstTriggerIndex];
      var otherTriggers = ImmutableObjectUtils.arrayRemoveElementAtIndex(version.triggers, firstTriggerIndex);
      return (
        <div className={this.props.disableWrapping ? "display-ellipsis" : ""}>
          {this.getLabelFromTrigger(firstTrigger)}
          {this.getNonRegexTriggerLabelsFromTriggers(otherTriggers)}
          {this.getRegexTriggerLabelFromTriggers(otherTriggers)}
        </div>
      );
    },

    getDataTypeLabelFromVersion: function(version) {
      return (
        <div className={"type-italic " + (this.props.disableWrapping ? "display-ellipsis" : "")}>
          <span className="link">{version.getDataTypeName()}</span>
          {this.props.labelDataType ? (
            <span className="type-weak"> (data type)</span>
          ) : null}
        </div>
      );
    },

    getLabelFromVersion: function(version) {
      if (version.isDataType()) {
        return this.getDataTypeLabelFromVersion(version);
      } else {
        return this.getTriggersFromVersion(version);
      }
    },

    getDescriptionFromVersion: function(version) {
      if (version.description) {
        return (
          <div className="type-italic type-weak pbxs ">{version.description}</div>
        );
      }
    },

    render: function() {
      if (this.props.disableLink) {
        return (
          <div>
            {this.getLabelFromVersion(this.props.version)}
            {this.getDescriptionFromVersion(this.props.version)}
          </div>
        );
      } else {
        return (
          <div>
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
