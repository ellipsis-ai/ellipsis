define(function(require) {
  var React = require('react'),
    SVGHamburger = require('../svg/hamburger');

  return React.createClass({
    propTypes: {
      behaviorCount: React.PropTypes.number.isRequired,
      onClick: React.PropTypes.func.isRequired
    },

    getButtonLabel: function() {
      if (this.props.behaviorCount === 1) {
        return `1 action in this skill`;
      } else {
        return `${this.props.behaviorCount} actions in this skill`;
      }
    },

    render: function() {
      return (
        <div className="align-r ptxl">
          <button type="button" className="button-tab" onClick={this.props.onClick}>
            <span>{this.getButtonLabel()}</span>
            <span className="display-inline-block align-b mlm" style={{height: "24px"}}>
              <SVGHamburger />
            </span>
          </button>
        </div>
      );
    }
  });
});
