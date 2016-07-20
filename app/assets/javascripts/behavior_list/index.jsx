define(function(require) {
  var React = require('react'),
    Formatter = require('../formatter'),
    SVGInstalled = require('../svg/installed');

  return React.createClass({
    propTypes: {
      behaviorVersions: React.PropTypes.arrayOf(React.PropTypes.object).isRequired
    },

    getFirstTriggerTextFromVersion: function(version) {
      var firstTrigger = version.triggers[0];
      return firstTrigger && firstTrigger.text ? firstTrigger.text : "";
    },

    getFirstTriggerFromVersion: function(version) {
      var firstTriggerText = this.getFirstTriggerTextFromVersion(version);
      var text = firstTriggerText ?
        (<span className="type-monospace">{firstTriggerText}</span>) :
        (<span className="type-italic">(New behavior)</span>);
      return (
        <span className="link">{text}</span>
      );
    },

    getOtherTriggersFromVersion: function(version) {
      return version.triggers.slice(1).map(function(trigger, index) {
        if (trigger.text) {
          return (
            <span className="type-monospace" key={"trigger" + (index + 1)}>
              <span className="type-disabled"> · </span>
              <span className="type-weak">{trigger.text}</span>
            </span>
          );
        } else {
          return null;
        }
      }, this);
    },

    getTriggersFromVersion: function(version) {
      return (
        <div>
          <a href={jsRoutes.controllers.ApplicationController.editBehavior(version.behaviorId).url}
            className="link-block">
            {this.getFirstTriggerFromVersion(version)}
            {this.getOtherTriggersFromVersion(version)}
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
        var t1 = this.getFirstTriggerTextFromVersion(version1).toLowerCase();
        var t2 = this.getFirstTriggerTextFromVersion(version2).toLowerCase();
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
        <tr key={"version" + index}>
          <td width="1" className={"prm" + this.getTableRowClasses(index)}>
            {this.getImportedStatusFromVersion(version)}
          </td>
          <td className={"type-s" + this.getTableRowClasses(index)}>
            {this.getTriggersFromVersion(version)}
          </td>
          <td className={"plm type-s display-ellipsis align-r" + this.getTableRowClasses(index)}>
            {Formatter.formatTimestampRelativeIfRecent(version.createdAt)}
          </td>
        </tr>
      );
    },

    getTaskRows: function() {
      var tasks = this.getTasks();
      if (tasks.length > 0) {
        return (
          <tbody>
            <tr>
              <th className="ptl type-l pbs" colSpan="2">What Ellipsis can do</th>
              <th className="type-label align-r pbs">Last modified</th>
            </tr>
            {this.getTasks().map(this.getVersionRow, this)}
          </tbody>
        );
      }
    },

    getKnowledgeRows: function() {
      var knowledge = this.getKnowledge();
      if (knowledge.length > 0) {
        return (
          <tbody>
            <tr>
              <th className="ptxxl type-l pbs" colSpan="2">What Ellipsis knows</th>
              <th className="type-label align-r pbs">Last modified</th>
            </tr>
            {this.getKnowledge().map(this.getVersionRow, this)}
          </tbody>
        );
      }
    },

    render: function() {
      if (this.props.behaviorVersions.length > 0) {
        return (
          <table>
            {this.getTaskRows()}
            {this.getKnowledgeRows()}
          </table>
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
