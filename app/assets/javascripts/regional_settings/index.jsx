define(function(require) {
  const React = require('react'),
    autobind = require('../lib/autobind');

  class RegionalSettings extends React.Component {
    constructor(props) {
      super(props);
      autobind(this);
    }

    render() {
      return (
        <div>
        </div>
      );
    }
  }

  RegionalSettings.propTypes = {
    csrfToken: React.PropTypes.string.isRequired
    // string: React.PropTypes.string.isRequired,
    // callback: React.PropTypes.func.isRequired,
    // children: React.PropTypes.node.isRequired
  };

  return RegionalSettings;
});
