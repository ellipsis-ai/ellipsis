define(function(require) {
  var React = require('react'),
    BehaviorEnvironmentVariableChooser = require('./behavior_environment_variable_chooser');

  return React.createClass({

    propTypes: {
      envVariableNames: React.PropTypes.arrayOf(React.PropTypes.string).isRequired,
      accessKeyName: React.PropTypes.string,
      secretKeyName: React.PropTypes.string,
      regionName: React.PropTypes.string,
      onChange: React.PropTypes.func.isRequired,
      onRemoveAWSConfig: React.PropTypes.func.isRequired
    },

    render: function () {

      return (

        <div className="columns pvl">

          <div className="column column-three-quarters">

            <BehaviorEnvironmentVariableChooser
              label="Access&nbsp;Key"
              property="accessKeyName"
              chosenName={this.props.accessKeyName}
              envVariableNames={this.props.envVariableNames}
              onChange={this.props.onChange}
              />

            <BehaviorEnvironmentVariableChooser
              label="Secret&nbsp;Key"
              property="secretKeyName"
              chosenName={this.props.secretKeyName}
              envVariableNames={this.props.envVariableNames}
              onChange={this.props.onChange}
              />

            <BehaviorEnvironmentVariableChooser
              label="Region"
              property="regionName"
              chosenName={this.props.regionName}
              envVariableNames={this.props.envVariableNames}
              onChange={this.props.onChange}
              />

          </div>

          <div className="column column-one-quarter pr-symbol align-r">
            <button type="button" className="button-s" onClick={this.props.onRemoveAWSConfig}>Don't use AWS</button>
          </div>

        </div>
      );
    }
  });
});
