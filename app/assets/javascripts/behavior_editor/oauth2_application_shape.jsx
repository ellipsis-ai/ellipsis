define(function() {
  var React = require('react');

  return React.PropTypes.shape({
    apiId: React.PropTypes.string.isRequired,
    applicationId: React.PropTypes.string.isRequired,
    displayName: React.PropTypes.string,
    keyName: React.PropTypes.string,
    scope: React.PropTypes.string
  });
});
