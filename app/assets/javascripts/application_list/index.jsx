define(function(require) {
  var React = require('react');

  return React.createClass({
    displayName: 'ApplicationList',
    propTypes: {
      csrfToken: React.PropTypes.string.isRequired,
      teamId: React.PropTypes.string.isRequired
    },

    render: () => {
      return (
        <p>Thar be applications</p>
      );
    }
  });
});
