define(function(require) {
  var React = require('react'),
    FormInput = require('./input'),
    SVGSearch = require('../svg/search');

  return React.createClass({
    displayName: 'FormSearch',
    propTypes: {
      isSearching: React.PropTypes.bool,
      onChange: React.PropTypes.func.isRequired,
      value: React.PropTypes.string,
      className: React.PropTypes.string
    },

    getRemainingProps: function() {
      var props = Object.assign({}, this.props);
      delete props.isSearching;
      delete props.className;
      return props;
    },

    clearValue: function() {
      this.props.onChange("");
    },

    render: function() {
      return (
        <div className="columns columns-elastic">
          <div className="column column-expand prn">
            <div className={"position-relative " + (this.props.isSearching ? "pulse-faded" : "")}>
              <div>
                <FormInput
                  {...this.getRemainingProps()}
                  className={"form-input-icon form-input-with-button " + (this.props.className || "")}
                  onEscKey={this.clearValue}
                />
              </div>
              <div
                className={
                  "position-absolute position-top-left position-z-above align-button mls type-weak"
                }
                style={{ height: "24px" }}
              >
                <SVGSearch />
              </div>
            </div>
          </div>
          <div className="column column-shrink">
            <button type="button"
              className="button-shrink button-with-form-input"
              onClick={this.clearValue}
              disabled={!this.props.value}
            >
              Clear
            </button>
          </div>
        </div>
      );
    }
  });
});
