import * as React from 'react';
import FormInput, {FocusableTextInputInterface, FormInputProps} from './input';
import SVGSearch from '../svg/search';
import SVGX from '../svg/x';
import autobind from "../lib/autobind";

interface SearchProps {
  isSearching?: boolean,
  withResults?: boolean
}

type Props = FormInputProps & SearchProps;

class FormSearch extends React.Component<Props> implements FocusableTextInputInterface {
  input: Option<FormInput>;

  constructor(props: Props) {
    super(props);
    autobind(this);
  }

    getRemainingProps(): FormInputProps {
      var props: FormInputProps & Props = Object.assign({}, this.props);
      delete props.isSearching;
      delete props.className;
      return props;
    }

    onEscKey(): void {
      this.clearValue();
      if (this.props.onEscKey) {
        this.props.onEscKey();
      }
    }

    clearValue(): void {
      const hasCurrentValue = Boolean(this.props.value);
      this.props.onChange("");
      if (hasCurrentValue) {
        this.focus();
      } else {
        this.blur();
      }
    }

    focus(): void {
      if (this.input) {
        this.input.focus();
      }
    }

    blur(): void {
      if (this.input) {
        this.input.blur();
      }
    }

    select(): void {
      if (this.input) {
        this.input.select();
      }
    }

    getClassNames(): Array<string> {
      return this.props.className ? this.props.className.split(" ") : [];
    }

    isSmall(): boolean {
      return this.getClassNames().includes("form-input-s");
    }

    isBorderless(): boolean {
      return this.getClassNames().includes("form-input-borderless");
    }

    render() {
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
                  onEscKey={this.onEscKey}
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
}

export default FormSearch;
