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
      className: React.PropTypes.string,
      withResults: React.PropTypes.bool
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

    focus: function() {
      if (this.input) {
        this.input.focus();
      }
    },

    render: function() {
      return (
        <div className={"columns columns-elastic " + (this.props.withResults ? " mbneg1 position-relative position-z-above " : "")}>
          <div className="column column-expand prn">
            <div className={"position-relative " + (this.props.isSearching ? "pulse-faded" : "")}>
              <div>
                <FormInput
                  ref={(input) => this.input = input}
                  {...this.getRemainingProps()}
                  className={
                    "form-input-icon form-input-with-button " +
                    (this.props.withResults ? " border-radius-bottom-none " : "") +
                    (this.props.className || "")
                  }
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
              className={
                "button-shrink button-with-form-input " +
                (this.props.withResults ? " border-radius-bottom-none " : "")
              }
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
