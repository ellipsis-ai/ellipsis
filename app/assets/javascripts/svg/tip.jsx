define(function(require) {
var React = require('react');

return React.createClass({
  propTypes: {
    label: React.PropTypes.string
  },
  label: function() {
    return this.props.label || '→';
  },
  render: function() {
    return (
      <svg role="img" aria-label={this.label()} height="100%" viewBox="0 0 22 24">
        <title>{this.label()}</title>
        <g id="Page-1" stroke="none" strokeWidth="1" fill="none" fillRule="evenodd">
          <g id="tip">
            <path d="M11,22 C16.5228475,22 21,17.5228475 21,12 C21,6.4771525 16.5228475,2 11,2 C5.4771525,2 1,6.4771525 1,12 C1,17.5228475 5.4771525,22 11,22 Z" id="Combined-Shape" fill="currentColor"></path>
            <polygon id="➔" fill="#FFFFFF" points="17.2930908 11.9245605 12.2943115 16.9575195 9.02160645 16.9575195 12.9180908 13.1037598 5 13.1037598 5 10.7539062 12.9180908 10.7539062 9.02160645 6.90014648 12.2943115 6.90014648"></polygon>
          </g>
        </g>
      </svg>
    );
  }
});

});
