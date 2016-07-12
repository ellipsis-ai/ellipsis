define(function(require) {
  var React = require('react'),
    Input = require('./input');

  return React.createClass({
    propTypes: {
      onCancelClick: React.PropTypes.func.isRequired,
      vars: React.PropTypes.arrayOf(React.PropTypes.object).isRequired
    },

    getVars: function() {
      return this.state.vars;
    },

    getInitialState: function() {
      return {
        vars: this.props.vars
      }
    },

    onChangeVarValue: function(modifiedIndex, newValue) {
      var newVars = this.state.vars.map(function(v, index) {
        if (index === modifiedIndex) {
          return {
            name: v.name,
            value: newValue
          }
        } else {
          return v;
        }
      });
      this.setState({
        vars: newVars
      });
    },

    render: function() {
      return (
        <div className="box-action">
          <div className="container phn">
            <p>
              <span>Set environment variables to hold secure information like access keys for other services </span>
              <span>that may be used by multiple behaviors.</span>
            </p>

            <div className="form-grouped-inputs">
            {this.getVars().map(function(v, index) {
              return (
                <div className="columns" key={"envVar" + index}>
                  <div className="column column-one-quarter prn">
                    <input type="text"
                      className="form-input form-input-left"
                      placeholder="ENVIRONMENT_VARIABLE_NAME"
                      value={v.name}
                      readOnly={true}
                    />
                  </div>
                  <div className="column column-one-quarter pln">
                    <Input
                      className="form-input-right"
                      placeholder="Enter value"
                      value={v.value}
                      onChange={this.onChangeVarValue.bind(this, index)}
                    />
                  </div>
                </div>
              );
            }, this)}
            </div>

            <div className="columns mtm">
              <div className="column column-one-half">
                <button type="button" className="button-primary mrs">Save</button>
                <button type="button" onClick={this.props.onCancelClick}>Cancel</button>
              </div>
            </div>

          </div>
        </div>
      );
    }
  });
});
