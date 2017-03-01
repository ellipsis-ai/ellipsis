define(function(require) {
  var React = require('react'),
    SVGCollapse = require('../svg/collapse');

  return React.createClass({
    displayName: 'CollapseButton',
    propTypes: {
      direction: React.PropTypes.string,
      onClick: React.PropTypes.func.isRequired
    },

    onClick: function() {
      this.props.onClick();
    },

    render: function() {
      return (
        <button type="button" className="button-raw type-weak align-t" onClick={this.onClick} style={{ height: "22px" }}>
          <SVGCollapse direction={this.props.direction} />
        </button>
      );
    }
  });
});
