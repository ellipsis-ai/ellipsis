define(function(require) {
  var React = require('react')

  return React.createClass({
    propTypes: {
      label: React.PropTypes.string.isRequired,
      property: React.PropTypes.string.isRequired,
      chosenName: React.PropTypes.string,
      envVariableNames: React.PropTypes.arrayOf(React.PropTypes.string).isRequired,
      onChange: React.PropTypes.func.isRequired
    },

    onChange: function(event) {
      event.preventDefault();
      var property = this.props.property;
      var chosen = event.target.value;
      this.props.onChange(property, chosen);
    },

    render: function () {
      console.log("chosen env var for " + this.props.property + ": " + this.props.chosenName);
      return (
        <div className="pvs">

          <span>{this.props.label}</span>
          <select name={this.props.property} value={this.props.chosenName} onChange={this.onChange}>
            <option value={undefined}>-- Pick an environment variable --</option>
            {this.props.envVariableNames.map(function(envVarName) {
              return (
                <option value={envVarName} key={'AWSEnvVarName-' + this.props.property + '-' + envVarName}>{envVarName}</option>
              );
            }, this)}
          </select>

        </div>
      );
    }
  });
});
