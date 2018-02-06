import * as React from 'react';

const SVGWarning = React.createClass({
  propTypes: {
    label: React.PropTypes.string,
    inverted: React.PropTypes.bool
  },
  label: function() {
    return this.props.label || '!';
  },
  getShapeColor: function() {
    return this.props.inverted ? 'currentColor' : '#FFFFFF';
  },
  getBackgroundColor: function() {
    return this.props.inverted ? '#FFFFFF' : 'currentColor';
  },
  render: function() {
    return (
      <svg role="img" aria-label={this.label()} height="100%" viewBox="0 0 22 24">
        <title>{this.label()}</title>
        <g id="Page-1" stroke="none" strokeWidth="1" fill="none" fillRule="evenodd">
          <g id="warning">
            <path d="M11,22 C16.5228475,22 21,17.5228475 21,12 C21,6.4771525 16.5228475,2 11,2 C5.4771525,2 1,6.4771525 1,12 C1,17.5228475 5.4771525,22 11,22 Z" id="Combined-Shape" fill={this.getBackgroundColor()} />
            <polygon id="!" fill={this.getShapeColor()} points="10.0433789 13.6816402 11.9633789 13.6816402 12.38 8.26 12.48 5.6 9.52 5.6 9.62 8.26" />
            <path d="M9.22,17.4 C9.22,18.44 9.98,19.24 11,19.24 C12.02,19.24 12.78,18.44 12.78,17.4 C12.78,16.34 12.02,15.54 11,15.54 C9.98,15.54 9.22,16.34 9.22,17.4 L9.22,17.4 Z" id="!" fill={this.getShapeColor()} />
          </g>
        </g>
      </svg>
    );
  }
});

export default SVGWarning;
