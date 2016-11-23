define(function(require) {
  var React = require('react'),
    BehaviorVersion = require('../models/behavior_version'),
    ImmutableObjectUtils = require('../immutable_object_utils');

  return React.createClass({
    displayName: 'BehaviorName',
    propTypes: {
      version: React.PropTypes.instanceOf(BehaviorVersion).isRequired
    },

    getLabelFromTrigger: function(trigger) {
      return trigger && trigger.text ?
        (<span className="link type-monospace">{trigger.displayText}</span>) :
        (<span className="link type-italic">(New skill)</span>);
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
        <div>
          <a href={jsRoutes.controllers.BehaviorEditorController.edit(version.behaviorId).url}
            className="link-block">
            {this.getLabelFromTrigger(firstTrigger)}
            {this.getNonRegexTriggerLabelsFromTriggers(otherTriggers)}
            {this.getRegexTriggerLabelFromTriggers(otherTriggers)}
          </a>
        </div>
      );
    },

    getDescriptionFromVersion: function(version) {
      if (version.description) {
        return (
          <div className="type-italic type-weak pbxs ">{version.description}</div>
        );
      }
    },

    render: function() {
      return (
        <div>
          {this.getTriggersFromVersion(this.props.version)}
          {this.getDescriptionFromVersion(this.props.version)}
        </div>
      );
    }
  });
});
