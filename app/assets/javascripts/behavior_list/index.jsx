define(function(require) {
  var React = require('react'),
    Formatter = require('../formatter'),
    ImmutableObjectUtils = require('../immutable_object_utils'),
    SVGInstalled = require('../svg/installed');

  return React.createClass({
    propTypes: {
      behaviorVersions: React.PropTypes.arrayOf(React.PropTypes.object).isRequired
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
        (<span className="link type-monospace">{firstTrigger.text}</span>) :
        (<span className="link type-italic">(New behavior)</span>);
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
              <span className="type-weak">{trigger.text}</span>
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
            <span className="type-weak type-italic">{text}</span>
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
          <a href={jsRoutes.controllers.ApplicationController.editBehavior(version.behaviorId).url}
            className="link-block">
            {firstTrigger.label}
            {this.getNonRegexTriggerLabelsFromTriggers(otherTriggers)}
            {this.getRegexTriggerLabelFromTriggers(otherTriggers)}
          </a>
        </div>
      );
    },

    getImportedStatusFromVersion: function(version) {
      if (version.importedId) {
        return (
          <span title="Installed from ellipsis.ai" className="mtxs display-inline-block" style={{ height: 18 }}>
            <SVGInstalled />
          </span>
        );
      }
    },

    getGroupedVersions: function() {
      var tasks = [];
      var knowledge = [];
      this.props.behaviorVersions.forEach(function(version) {
        if (version.functionBody) {
          tasks.push(version);
        } else {
          knowledge.push(version);
        }
      }, this);
      return {
        tasks: this.sortVersionsByFirstTrigger(tasks),
        knowledge: this.sortVersionsByFirstTrigger(knowledge)
      };
    },

    sortVersionsByFirstTrigger: function(versions) {
      return versions.sort(function(version1, version2) {
        var t1 = this.getDisplayTriggerFromVersion(version1).text.toLowerCase();
        var t2 = this.getDisplayTriggerFromVersion(version2).text.toLowerCase();
        if (t1 < t2) {
          return -1;
        } else if (t1 > t2) {
          return 1;
        } else {
          return 0;
        }
      }.bind(this));
    },

    getKnowledge: function() {
      return this.state.groupedVersions.knowledge;
    },

    getTasks: function() {
      return this.state.groupedVersions.tasks;
    },

    getInitialState: function() {
      return {
        groupedVersions: this.getGroupedVersions()
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
          <div className={"column column-expand type-s type-wrap-words mobile-border-top mobile-pts mobile-pbn" + this.getTableRowClasses(index)}>
            {this.getTriggersFromVersion(version)}
          </div>
          <div className={"column column-shrink type-s display-ellipsis align-r prxs mobile-border-none mobile-ptn mobile-pbxs" + this.getTableRowClasses(index)}>
            {Formatter.formatTimestampRelativeIfRecent(version.createdAt)}
          </div>
          <div className={"column column-shrink mobile-border-none mobile-ptn mobile-pbxs" + this.getTableRowClasses(index)}>
            {this.getImportedStatusFromVersion(version)}
          </div>
        </div>
      );
    },

    getTaskRows: function() {
      var tasks = this.getTasks();
      if (tasks.length > 0) {
        return (
          <div className="column-group">
            <div className="column-row type-bold">
              <div className="column column-expand ptl type-l pbs">What Ellipsis can do</div>
              <div className="column column-shrink type-label align-r pbs align-b">Last modified</div>
            </div>
            {this.getTasks().map(this.getVersionRow, this)}
          </div>
        );
      }
    },

    getKnowledgeRows: function() {
      var knowledge = this.getKnowledge();
      if (knowledge.length > 0) {
        return (
          <div className="column-group">
            <div className="column-row">
              <div className="column column-expand ptxxl type-l pbs">What Ellipsis knows</div>
              <div className="column column-shrink type-label align-r pbs align-b">Last modified</div>
            </div>
            {this.getKnowledge().map(this.getVersionRow, this)}
          </div>
        );
      }
    },

    render: function() {
      if (this.props.behaviorVersions.length > 0) {
        return (
          <div className="columns columns-elastic mobile-columns-float">
            {this.getTaskRows()}
            {this.getKnowledgeRows()}
          </div>
        );
      } else {
        return (
          <p className="type-l pvxl">
          Ellipsis doesn’t know any behaviors yet. Try installing some of the ones
          published by Ellipsis, or create a new one yourself.
          </p>
        );
      }
    }
  });
});
