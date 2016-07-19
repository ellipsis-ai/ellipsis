define(function(require) {
  var React = require('react');

  return React.createClass({
    propTypes: {
      behaviorVersions: React.PropTypes.arrayOf(React.PropTypes.object).isRequired
    },

    render: function() {
      return (
        <div>
          {this.props.behaviorVersions.map(function(version) {
            return (
              <p>
              {version.triggers.map(function(trigger) { return trigger.text; }).join(' · ')}
              </p>
            )
          })}
        </div>
      );
    }
  });
});
