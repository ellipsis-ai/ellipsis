if (typeof define !== 'function') { var define = require('amdefine')(module); }
define(function(require) {
var React = require('react');

return React.createClass({
  displayName: 'SVGXIcon',
  label: function() {
    return this.props.label || '×';
  },
  render: function() {
    return (
      <svg role="img" aria-label={this.label()} height="100%" viewBox="0 0 18 24">
        <title>{this.label()}</title>
        <g id="Page-1" stroke="none" strokeWidth="1" fill="none" fillRule="evenodd">
          <g id="delete" fill="currentColor">
            <polygon id="delete" points="3.878 19.16 9.026 13.976 14.174 19.16 16.802 16.496 11.69 11.348 16.802 6.164 14.174 3.5 9.026 8.684 3.878 3.5 1.25 6.164 6.362 11.348 1.25 16.496">
            </polygon>
          </g>
        </g>
      </svg>
    );
  }
});

});
