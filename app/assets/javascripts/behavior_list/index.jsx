define(function(require) {
  var React = require('react'),
    Formatter = require('../formatter');

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
        <a href={jsRoutes.controllers.ApplicationController.editBehavior(version.behaviorId).url}>{text}</a>
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
          {this.getFirstTriggerFromVersion(version)}
          {this.getOtherTriggersFromVersion(version)}
        </div>
      );
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
          <td className={this.getTableRowClasses(index)}>
          </td>
          <td className={"type-s" + this.getTableRowClasses(index)}>
            {this.getTriggersFromVersion(version)}
          </td>
          <td className={"type-s align-r" + this.getTableRowClasses(index)}>
            {Formatter.formatTimestampShort(version.createdAt)}
          </td>
        </tr>
      );
    },

    render: function() {
      return (
        <table>
          <thead>
            <tr>
              <th className="ptl type-l pbs" colSpan="2">What Ellipsis can do</th>
              <th className="type-label align-r pbs">Last modified</th>
            </tr>
          </thead>
          <tbody>
            {this.getTasks().map(this.getVersionRow, this)}
          </tbody>
          <thead>
            <tr>
              <th className="ptxxl type-l pbs" colSpan="2">What Ellipsis knows</th>
              <th className="type-label align-r pbs">Last modified</th>
            </tr>
          </thead>
          <tbody>
            {this.getKnowledge().map(this.getVersionRow, this)}
          </tbody>
        </table>
      );
    }
  });
});
