if (typeof define !== 'function') { var define = require('amdefine')(module); }
define(function(require) {
var React = require('react');

return React.createClass({
  displayName: 'SVGQuestionMark',
  render: function() {
    return (
      <svg aria-label="?" height="100%" viewBox="0 0 12 24">
        <title>?</title>
        <g id="Page-1" stroke="none" stroke-width="1" fill="none" fill-rule="evenodd">
          <g id="question" fill="currentColor">
            <path d="M4.22799999,14.168 L7.05599999,14.168 C6.63599999,10.752 11.452,10.024 11.452,6.384 C11.452,3.5 9.15599999,1.904 5.99199999,1.904 C3.77999999,1.904 1.98799999,2.912 0.699999988,4.312 L2.51999999,5.992 C3.44399999,5.096 4.47999999,4.536 5.76799999,4.536 C7.36399999,4.536 8.34399999,5.432 8.34399999,6.776 C8.34399999,9.24 3.58399999,10.304 4.22799999,14.168 L4.22799999,14.168 Z M4.0599101,18.8240449 C4.0599101,19.901618 4.81820224,20.62 5.79599999,20.62 C6.77379774,20.62 7.53208988,19.901618 7.53208988,18.8240449 C7.53208988,17.766427 6.77379774,17.068 5.79599999,17.068 C4.81820224,17.068 4.0599101,17.766427 4.0599101,18.8240449 L4.0599101,18.8240449 Z" id="?"></path>
          </g>
        </g>
      </svg>
    );
  }
});

});
