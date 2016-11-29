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
        return `Edit skill`;
      } else {
        return `Edit skill with ${this.props.behaviorCount} actions`;
      }
    },

    render: function() {
      return (
        <div>
          <button type="button" className="button-raw" onClick={this.props.onClick}>
            <span className="display-inline-block align-b mrm" style={{height: "24px"}}>
              <SVGHamburger />
            </span>
            <span>{this.getButtonLabel()}</span>
          </button>
        </div>
      );
    }
  });
});
