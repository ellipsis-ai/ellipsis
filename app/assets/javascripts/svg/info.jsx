define(function(require) {
  var React = require('react');

  return React.createClass({
    propTypes: {
      label: React.PropTypes.string
    },
    label: function() {
      return this.props.label || 'info';
    },
    render: function() {
      return (
        <svg role="img" aria-label={this.label()} height="100%" viewBox="0 0 22 24">
          <title>{this.label()}</title>
          <defs>
            <path d="M11,22 C16.5228475,22 21,17.5228475 21,12 C21,6.4771525 16.5228475,2 11,2 C5.4771525,2 1,6.4771525 1,12 C1,17.5228475 5.4771525,22 11,22 Z" id="path-1"></path>
            <mask id="mask-2" maskContentUnits="userSpaceOnUse" maskUnits="objectBoundingBox" x="0" y="0" width="20" height="20" fill="white">
              <use href="#path-1"></use>
            </mask>
          </defs>
          <g id="Page-1" stroke="none" strokeWidth="1" fill="none" fillRule="evenodd">
            <g id="info">
              <use id="Combined-Shape" stroke="currentColor" mask="url(#mask-2)" strokeWidth="3" strokeLinecap="square" href="#path-1"></use>
              <path d="M9.635,8.134 C9.635,8.792 10.139,9.268 10.797,9.268 C11.441,9.268 11.959,8.792 11.959,8.134 C11.959,7.49 11.441,7 10.797,7 C10.139,7 9.635,7.49 9.635,8.134 Z M11.791,11.102 C11.791,10.752 11.595,10.556 11.245,10.556 L10.419,10.556 C10.083,10.556 9.873,10.752 9.873,11.102 L9.873,16.114 C9.873,17.136 10.475,17.57 11.483,17.57 C11.721,17.57 11.903,17.556 12.001,17.542 C12.379,17.5 12.533,17.304 12.533,16.996 L12.533,16.184 C12.533,16.044 12.477,15.974 12.365,16.002 C12.365,16.002 12.309,16.016 12.211,16.016 C11.917,16.016 11.791,15.89 11.791,15.582 L11.791,11.102 Z" id="i" fill="currentColor"></path>
            </g>
          </g>
        </svg>
      );
    }
  });

});
