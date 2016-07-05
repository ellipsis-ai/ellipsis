define(function(require) {
  var React = require('react'),
    BehaviorEnvironmentVariableChooser = require('./behavior_environment_variable_chooser');

  return React.createClass({

    propTypes: {
      envVariableNames: React.PropTypes.arrayOf(React.PropTypes.string).isRequired,
      accessKeyName: React.PropTypes.string,
      secretKeyName: React.PropTypes.string,
      regionName: React.PropTypes.string,
      onChange: React.PropTypes.func.isRequired
    },

    render: function () {

      return (

        <div className="border-top border-left border-right border-radius-top pvs">

          <BehaviorEnvironmentVariableChooser
            label="AWS Access Key"
            property="accessKeyName"
            chosenName={this.props.accessKeyName}
            envVariableNames={this.props.envVariableNames}
            onChange={this.props.onChange}
            />

          <BehaviorEnvironmentVariableChooser
            label="AWS Secret Key"
            property="secretKeyName"
            chosenName={this.props.secretKeyName}
            envVariableNames={this.props.envVariableNames}
            onChange={this.props.onChange}
            />

          <BehaviorEnvironmentVariableChooser
            label="AWS Region"
            property="regionName"
            chosenName={this.props.regionName}
            envVariableNames={this.props.envVariableNames}
            onChange={this.props.onChange}
            />

        </div>
      );
    }
  });
});
