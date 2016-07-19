define(function(require) {
  var React = require('react'),
    Formatter = require('../formatter')

  return React.createClass({
    propTypes: {
      behaviorVersions: React.PropTypes.arrayOf(React.PropTypes.object).isRequired
    },

    getFirstTriggerFromVersion: function(version) {
      var firstTrigger = version.triggers[0];
      var text = firstTrigger && firstTrigger.text ?
        (<span className="type-monospace">{firstTrigger.text}</span>) :
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

    render: function() {
      return (
        <table>
          <thead>
            <tr>
              <th className="type-label pbs"></th>
              <th className="type-label pbs">When someone says</th>
              <th className="type-label pbs align-r">Last modified</th>
            </tr>
          </thead>
          <tbody>
            {this.props.behaviorVersions.map(function(version, index) {
              return (
                <tr key={"version" + index}>
                  <td className="pvs border-top">
                  </td>
                  <td className="pvs border-top">{this.getTriggersFromVersion(version)}</td>
                  <td className="pvs border-top type-s align-r">
                    {Formatter.formatTimestampShort(version.createdAt)}
                  </td>
                </tr>
              );
            }, this)}
          </tbody>
        </table>
      );
    }
  });
});
