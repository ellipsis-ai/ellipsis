define(function(require) {
  var React = require('react');

  return React.createClass({
    propTypes: {
      behaviorData: React.PropTypes.object.isRequired,
      triggers: React.PropTypes.arrayOf(React.PropTypes.object).isRequired
    },

    isFirstTriggerIndex: function(index) {
      return index === 0;
    },

    isLastTriggerIndex: function(index) {
      return index === this.props.triggers.length - 1;
    },

    render: function() {
      return (
        <div className="column column-expand type-s">
          {this.props.triggers.map(function(trigger, index) {
            return (
              <span key={"trigger" + index}
                className={
                  "type-monospace " +
                  (this.isFirstTriggerIndex(index) ? "" : "type-weak")
                }
              >
                <span className="type-wrap-words">{trigger.text}</span>
                <span className="type-disabled">
                  {this.isLastTriggerIndex(index) ? "" : " · "}
                </span>
              </span>

            );
          }, this)}
          <div className="type-italic type-weak">{this.props.behaviorData.description}</div>
        </div>
      );
    }
  });
});
