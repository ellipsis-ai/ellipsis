define(function(require) {
  var React = require('react');

  return React.createClass({
    propTypes: {
      label: React.PropTypes.string,
      direction: React.PropTypes.string
    },
    label: function() {
      return this.props.label || 'Collapse';
    },
    orientation: function() {
      var d = this.props.direction ? this.props.direction.toLowerCase() : "";
      if (d === 'down') {
        return 270;
      } else if (d === 'right') {
        return 180;
      } else if (d === 'up') {
        return 90;
      } else {
        return 0;
      }
    },
    transform: function() {
      return `translate(11, 11) rotate(${this.orientation()}) translate(-11, -11) translate(0, 0)`;
    },
    render: function() {
      return (
        <svg role="img" aria-label={this.label()} height="100%" viewBox="0 0 22 22">
          <title>{this.label()}</title>
          <g id="Page-1" stroke="none" strokeWidth="1" fill="none" fillRule="evenodd">
            <g id="collapse-left" fill="currentColor" transform={this.transform()}>
              <polygon id="arrow" points="7 11 12 17 14 17 9 11 14 5 12 5" />
              <rect id="top" x="0" y="0" width="22" height="1" />
              <rect id="right" x="0" y="0" width="1" height="22" />
              <rect id="bottom" x="0" y="21" width="22" height="1" />
            </g>
          </g>
        </svg>
      );
    }
  });

});
