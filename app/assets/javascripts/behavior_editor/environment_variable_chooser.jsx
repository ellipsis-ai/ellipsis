define(function(require) {
  var React = require('react'),
    Select = require('../form/select');

  return React.createClass({
    propTypes: {
      label: React.PropTypes.string.isRequired,
      property: React.PropTypes.string.isRequired,
      chosenName: React.PropTypes.string,
      envVariableNames: React.PropTypes.arrayOf(React.PropTypes.string).isRequired,
      onAddNew: React.PropTypes.func.isRequired,
      onChange: React.PropTypes.func.isRequired
    },

    onChange: function(newValue, selectedIndex) {
      var property = this.props.property;
      var isAddNewSelected = selectedIndex === this.props.envVariableNames.length + 1;
      if (isAddNewSelected) {
        this.props.onAddNew(property);
      } else {
        this.props.onChange(property, newValue);
      }
    },

    keyFor: function(envVarName) {
      return 'AWSEnvVarName-' + this.props.property + '-' + envVarName;
    },

    render: function () {
      return (
        <div className="column-row">
          <div className="column column-shrink align-r mobile-align-l mobile-column-full display-ellipsis type-label pbxs prs align-m">{this.props.label}</div>
          <div className="column pbxs mobile-pbm">
            <Select className="form-select-s min-width-20" name={this.props.property} value={this.props.chosenName || ""} onChange={this.onChange}>
              <option value="" key={this.keyFor("none")}>Select environment variable…</option>
              {this.props.envVariableNames.map(function(envVarName) {
                return (
                  <option value={envVarName} key={this.keyFor(envVarName)}>{envVarName}</option>
                );
              }, this)}
              <option value="" key="AWSNewEnvVarName">Add new…</option>
            </Select>
          </div>
        </div>
      );
    }
  });
});
