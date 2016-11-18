define(function(require) {
  var React = require('react'),
    Formatter = require('../formatter'),
    ImmutableObjectUtils = require('../immutable_object_utils'),
    Sort = require('../sort'),
    SVGInstalled = require('../svg/installed');

  return React.createClass({
    propTypes: {
      behaviorVersions: React.PropTypes.arrayOf(React.PropTypes.object).isRequired
    },

    getTriggerTextFromTrigger: function(trigger) {
      return trigger.requiresMention ?
        `...${trigger.text}` :
        trigger.text;
    },

    getDisplayTriggerFromVersion: function(version) {
      var firstTriggerIndex = version.triggers.findIndex(function(trigger) {
        return !!trigger.text && !trigger.isRegex;
      });
      if (firstTriggerIndex === -1) {
        firstTriggerIndex = 0;
      }
      var firstTrigger = version.triggers[firstTriggerIndex];
      var text = firstTrigger && firstTrigger.text ? firstTrigger.text : "";
      var label = text ?
        (<span className="link type-monospace">{this.getTriggerTextFromTrigger(firstTrigger)}</span>) :
        (<span className="link type-italic">(New skill)</span>);
      return {
        index: firstTriggerIndex,
        label: label,
        text: text
      };
    },

    getNonRegexTriggerLabelsFromTriggers: function(triggers) {
      return triggers.filter(function (trigger) {
        return !trigger.isRegex;
      }).map(function (trigger, index) {
        if (trigger.text) {
          return (
            <span className="type-monospace" key={"regularTrigger" + index}>
              <span className="type-disabled"> · </span>
              <span>{this.getTriggerTextFromTrigger(trigger)}</span>
            </span>
          );
        } else {
          return null;
        }
      }, this);
    },

    getRegexTriggerLabelFromTriggers: function(triggers) {
      var regexTriggerCount = triggers.filter(function(trigger) {
        return !!trigger.isRegex;
      }).length;

      var text = regexTriggerCount === 1 ?
          "also matches another pattern" :
          "also matches " + regexTriggerCount + " other patterns";

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
      var firstTrigger = this.getDisplayTriggerFromVersion(version);
      var otherTriggers = ImmutableObjectUtils.arrayRemoveElementAtIndex(version.triggers, firstTrigger.index);
      return (
        <div>
          <a href={jsRoutes.controllers.BehaviorEditorController.edit(version.behaviorId).url}
            className="link-block">
            {firstTrigger.label}
            {this.getNonRegexTriggerLabelsFromTriggers(otherTriggers)}
            {this.getRegexTriggerLabelFromTriggers(otherTriggers)}
          </a>
        </div>
      );
    },

    getDescriptionFromVersion: function(version) {
      if (version.description) {
        return (
          <div className="type-italic type-weak">{version.description}</div>
        );
      }
    },

    getImportedStatusFromVersion: function(version) {
      if (version.importedId) {
        return (
          <span title="Installed from ellipsis.ai" className="mtxs display-inline-block" style={{ width: 30, height: 18 }}>
            <SVGInstalled />
          </span>
        );
      }
    },

    getVersions: function() {
      return this.sortVersionsByFirstTrigger(this.props.behaviorVersions);
    },

    sortVersionsByFirstTrigger: function(versions) {
      return Sort.arrayAlphabeticalBy(versions, (item) => this.getDisplayTriggerFromVersion(item).text);
    },

    getInitialState: function() {
      return {
        versions: this.getVersions()
      };
    },

    getTableRowClasses: function(index) {
      if (index % 3 === 0) {
        return " pts border-top ";
      } else if (index % 3 === 2) {
        return " pbs ";
      } else {
        return "";
      }
    },

    getVersionRow: function(version, index) {
      return (
        <div className="column-row" key={"version" + index}>
          <div className={"column column-expand type-s type-wrap-words border-top pvxs"}>
            {this.getTriggersFromVersion(version)}
            {this.getDescriptionFromVersion(version)}
          </div>
          <div className={"column column-shrink type-s type-weak display-ellipsis align-r prxs border-top pvxs mobile-display-none"}>
            {Formatter.formatTimestampRelativeIfRecent(version.createdAt)}
          </div>
          <div className={"column column-shrink border-top pvxs mobile-display-none"}>
            {this.getImportedStatusFromVersion(version)}
          </div>
        </div>
      );
    },

    getVersionRows: function() {
      var versions = this.getVersions();
      if (versions.length > 0) {
        return (
          <div className="column-group">
            <div className="column-row type-bold">
              <div className="column column-expand ptl type-l pbs">What Ellipsis can do</div>
              <div className="column column-shrink type-label align-r pbs align-b mobile-display-none">Last modified</div>
            </div>
            {versions.map(this.getVersionRow, this)}
          </div>
        );
      }
    },

    render: function() {
      if (this.props.behaviorVersions.length > 0) {
        return (
          <div>
            <p><i><b>Tip:</b> mention Ellipsis in chat by starting a message with “…”</i></p>

            <div className="columns columns-elastic mobile-columns-float">
              {this.getVersionRows()}
            </div>
          </div>
        );
      } else {
        return (
          <p className="type-l pvxl">
          Ellipsis doesn’t know any skills yet. Try installing some of the ones
          published by Ellipsis, or create a new one yourself.
          </p>
        );
      }
    }
  });
});
