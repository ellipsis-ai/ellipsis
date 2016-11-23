define(function(require) {
  var React = require('react'),
    BehaviorName = require('../behavior_list/behavior_name'),
    BehaviorVersion = require('../models/behavior_version');

  return React.createClass({
    displayName: 'BehaviorSwitcher',
    propTypes: {
      behaviors: React.PropTypes.arrayOf(React.PropTypes.instanceOf(BehaviorVersion)).isRequired
    },

    render: function() {
      return (
        <div className="position-fixed-right position-z-front bg-white pal">
          {this.props.behaviors.map((version) => (
            <BehaviorName key={`behavior-${version.behaviorId}`} version={version} />
          ))}
        </div>
      );
    }
  });
});
