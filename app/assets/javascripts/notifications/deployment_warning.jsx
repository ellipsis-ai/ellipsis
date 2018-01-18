define(function(require) {
  const React = require('react');
  const autobind = require('../lib/autobind');

  class NotificationForDeploymentWarning extends React.Component {

    constructor(props) {
      super(props);
      autobind(this);
      this.state = {
        isDeploying: false
      };
    }

    deploy() {
      this.setState({
        isDeploying: true
      }, () => {
        const detail = this.props.details.find((detail) => detail.type === "saved_version_not_deployed");
        detail.onClick(() => {
          this.setState({ isDeploying: false });
        });
      });
    }

    isDeploying() {
      return this.state && this.state.isDeploying;
    }

    render() {
      return (
        <span>
          <span className="type-label">Warning: </span>
          <span className="mrs">This latest version is not available for everyone to use until it has been deployed. </span>
          <button className="button-s button-inverted" type="button" onClick={this.deploy}>
            {this.isDeploying() ? "Deploying…" : "Deploy now"}
          </button>
        </span>
      );
    }
  }

  NotificationForDeploymentWarning.propTypes = {
    details: React.PropTypes.arrayOf(React.PropTypes.shape({
      kind: React.PropTypes.string.isRequired,
      type: React.PropTypes.string.isRequired,
      onClick: React.PropTypes.func.isRequired
    })).isRequired
  };

  return NotificationForDeploymentWarning;
});
