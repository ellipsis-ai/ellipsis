define(function(require) {
  var React = require('react'),
    EnvironmentVariableChooser = require('./environment_variable_chooser'),
    HelpButton = require('./help_button');

  return React.createClass({

    propTypes: {
      envVariableNames: React.PropTypes.arrayOf(React.PropTypes.string).isRequired,
      accessKeyName: React.PropTypes.string,
      secretKeyName: React.PropTypes.string,
      regionName: React.PropTypes.string,
      onChange: React.PropTypes.func.isRequired,
      onRemoveAWSConfig: React.PropTypes.func.isRequired,
      onToggleHelp: React.PropTypes.func.isRequired,
      helpVisible: React.PropTypes.bool.isRequired,
      onAddNew: React.PropTypes.func.isRequired
    },

    render: function () {

      return (

        <div className="columns columns-elastic">

          <div className="column column-expand">
            <p>
              <span className="mrs">Set your AWS access key and region.</span>
              <HelpButton onClick={this.props.onToggleHelp} toggled={this.props.helpVisible} />
            </p>

            <EnvironmentVariableChooser
              label="Access key ID"
              property="accessKeyName"
              chosenName={this.props.accessKeyName}
              envVariableNames={this.props.envVariableNames}
              onAddNew={this.props.onAddNew}
              onChange={this.props.onChange}
              />

            <EnvironmentVariableChooser
              label="Secret access key"
              property="secretKeyName"
              chosenName={this.props.secretKeyName}
              envVariableNames={this.props.envVariableNames}
              onAddNew={this.props.onAddNew}
              onChange={this.props.onChange}
              />

            <EnvironmentVariableChooser
              label="Region"
              property="regionName"
              chosenName={this.props.regionName}
              envVariableNames={this.props.envVariableNames}
              onAddNew={this.props.onAddNew}
              onChange={this.props.onChange}
              />

          </div>

          <div className="column column-shrink">
            <button type="button" className="button-s display-ellipsis" onClick={this.props.onRemoveAWSConfig}>Remove AWS</button>
          </div>

        </div>
      );
    }
  });
});
