define(function(require) {
var React = require('react');

return React.createClass({
  propTypes: {
    label: React.PropTypes.string
  },
  label: function() {
    return this.props.label || '▾';
  },
  render: function() {
    return (
      <svg aria-label={this.label()} height="100%" viewBox="0 0 8 16">
        <title>{this.label()}</title>
        <g id="Page-1" stroke="none" strokeWidth="1" fill="none" fillRule="evenodd">
          <g id="dropdown_triangles" fill="currentColor">
            <polygon id="Triangle" points="4 2 7 7 1 7" />
            <polygon id="Triangle" transform="translate(4.000000, 12.500000) scale(1, -1) translate(-4.000000, -12.500000) "
              points="4 10 7 15 1 15" />
          </g>
        </g>
      </svg>
    );
  }
});

});
