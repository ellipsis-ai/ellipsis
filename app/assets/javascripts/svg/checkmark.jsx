define(function(require) {
var React = require('react');

  return React.createClass({
    displayName: 'SVGCheckmark',
    propTypes: {
      label: React.PropTypes.string
    },
    label: function() {
      return this.props.label || '✓';
    },
    render: function() {
      return (
        <svg role="img" aria-label={this.label()} height="100%" viewBox="0 0 22 24">
          <title>{this.label()}</title>
          <g id="Page-1" stroke="none" strokeWidth="1" fill="none" fillRule="evenodd">
            <g id="checkmark">
              <g transform="translate(1.000000, 2.000000)">
                <circle id="Oval-1" fill="currentColor" cx="10" cy="10" r="10" />
                <path d="M9.545,15.06 C10.745,10.74 12.695,7.755 14.72,5.85 L13.37,4.68 C11.33,6.75 9.515,10.065 8.45,13.65 L8.39,13.65 C7.895,12.405 7.295,11.13 6.515,9.9 L5.105,10.815 C6.17,12.3 6.845,13.605 7.475,15.3 L9.545,15.06 Z" id="✓" fill="#FFFFFF" />
              </g>
            </g>
          </g>
        </svg>
      );
    }
  });

});
