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

    keyFor: function(envVarName) {
      return 'AWSEnvVarName-' + this.props.property + '-' + envVarName;
    },

    render: function () {
      return (
        <div className="pvs">

          <span>{this.props.label}</span>
          <select name={this.props.property} value={this.props.chosenName || ""} onChange={this.onChange}>
            <option value="" key={this.keyFor("none")}>-- Pick an environment variable --</option>
            {this.props.envVariableNames.map(function(envVarName) {
              return (
                <option value={envVarName} key={this.keyFor(envVarName)}>{envVarName}</option>
              );
            }, this)}
          </select>

        </div>
      );
    }
  });
});
