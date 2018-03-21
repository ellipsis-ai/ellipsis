import * as React from 'react';
import FormInput from './input';
import SVGSearch from '../svg/search';
import SVGX from '../svg/x';

const FormSearch = React.createClass({
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
      const hasCurrentValue = Boolean(this.props.value);
      this.props.onChange("", () => {
        if (hasCurrentValue) {
          this.focus();
        } else {
          this.blur();
        }
      });
    },

    focus: function() {
      if (this.input) {
        this.input.focus();
      }
    },

    blur: function() {
      if (this.input) {
        this.input.blur();
      }
    },

    getClassNames: function() {
      return this.props.className ? this.props.className.split(" ") : [];
    },

    isSmall: function() {
      return this.getClassNames().includes("form-input-s");
    },

    isBorderless: function() {
      return this.getClassNames().includes("form-input-borderless");
    },

    render: function() {
      return (
        <div className={"columns columns-elastic position-relative " + (this.props.withResults ? " mbneg1 position-z-above " : "")}>
          <div className="column column-expand prn">
            <div className={"position-relative " + (this.props.isSearching ? "pulse-faded" : "")}>
              <div>
                <FormInput
                  ref={(input) => this.input = input}
                  {...this.getRemainingProps()}
                  className={
                    `form-input-icon-left form-input-with-button ${
                      this.props.withResults ? " border-radius-bottom-none " : ""
                    } ${
                      this.getClassNames().join(" ")
                    }`
                  }
                  onEscKey={this.clearValue}
                />
              </div>
              <div
                className={
                  `position-absolute position-top-left position-z-above type-weak ${
                    this.isSmall() ? "align-button-s" : "align-button"
                  } ${
                    this.isBorderless() ? "" : "mls"
                  }`
                }
                style={{ height: this.isSmall() ? "18px" : "24px" }}
              >
                <SVGSearch />
              </div>
            </div>
          </div>
          <div className={`type-weak ${
            this.isBorderless() ? "position-absolute position-top-right position-z-above" : "column column-shrink"
          } ${
            this.isBorderless() && !this.props.value ? "display-none" : "fade-in"
          }`}>
            <button type="button"
              className={
                `button-shrink button-with-form-input ${
                  this.props.withResults ? "border-radius-bottom-none" : ""
                } ${
                  this.isSmall() ? "button-s" : ""
                } ${
                  this.isBorderless() ? "button-subtle" : ""
                }`
              }
              onClick={this.clearValue}
              disabled={!this.props.value}
            >
              <SVGX label="Clear search" />
            </button>
          </div>
        </div>
      );
    }
});

export default FormSearch;
