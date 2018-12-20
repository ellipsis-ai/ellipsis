import * as React from 'react';
import autobind from "../lib/autobind";
import {OptionHTMLAttributes} from "react";
import {FocusableTextInputInterface} from "./input";

interface Props {
  className?: Option<string>,
  name?: string,
  value?: string,
  children: React.ReactNode,
  onChange: (newValue: string, newIndex: number) => void,
  disabled?: Option<boolean>,
  size?: number,
  withSearch?: Option<boolean>
}

interface State {
  focused: boolean
}

export interface SelectOption {
  key: string
  value: string
  label: string
}

class Select extends React.Component<Props, State> implements FocusableTextInputInterface {
    selectElement: Option<HTMLSelectElement>;

    constructor(props: Props) {
      super(props);
      autobind(this);
      this.state = {
        focused: false
      };
      this.selectElement = null;
    }

    componentDidUpdate(prevProps: Props): void {
      if (this.props.size && prevProps.value !== this.props.value) {
        this.scrollToSelectedOption();
      }
    }

    scrollToSelectedOption(): void {
      const selector = this.selectElement;
      const selectedOption = selector && selector.selectedIndex >= 0 ? selector.options[selector.selectedIndex] : null;
      /* Safari returns 0 for offset positioning of <option> elements */
      if (!selectedOption || !selectedOption.offsetHeight) {
        return;
      }

      if (selector) {
        const selectorTop = selector.scrollTop;
        const selectorBottom = selectorTop + selector.offsetHeight;
        const optionTop = selectedOption.offsetTop;
        const optionBottom = optionTop + selectedOption.offsetHeight;
        if (optionBottom < selectorTop || optionTop > selectorBottom) {
          selector.scrollTop = optionTop;
        }
      }
    }

    onChange(event: React.ChangeEvent<HTMLSelectElement>): void {
     this.props.onChange(event.target.value, event.target.selectedIndex);
    }

    onFocus(): void {
      this.setState({ focused: true });
    }

    onBlur(): void {
      this.setState({ focused: false });
    }

    selectNextItem(): Option<number> {
      if (this.selectElement && this.selectElement.selectedIndex + 1 < this.selectElement.options.length) {
        this.selectElement.selectedIndex++;
      }
      return this.selectElement ? this.selectElement.selectedIndex : null;
    }

    selectPreviousItem(): Option<number> {
      if (this.selectElement && this.selectElement.selectedIndex > 0) {
        this.selectElement.selectedIndex--;
      }
      return this.selectElement ? this.selectElement.selectedIndex: null;
    }

    getCurrentValue(): Option<string> {
      return this.selectElement ? this.selectElement.value : null;
    }

    getCurrentIndex(): Option<number> {
      return this.selectElement ? this.selectElement.selectedIndex : null;
    }

    focus(): void {
      if (this.selectElement) {
        this.selectElement.focus();
      }
    }

    blur(): void {
      if (this.selectElement) {
        this.selectElement.blur();
      }
    }

    select(): void {
      this.focus();
    }

    render() {
      return (
        <div
          className={`${
            this.props.size ? "form-multi-select-container" : "form-select"
          } ${
            this.state.focused ? "form-select-focus" : ""
          } ${
            this.props.className || ""
          }`}
        >
          <select ref={(el) => this.selectElement = el}
            className={
              (this.props.size ? " form-multi-select " : " form-select-element ") +
              (this.props.withSearch ? " border-radius-bottom " : "")
            }
            name={this.props.name}
            value={this.props.value}
            onChange={this.onChange}
            onFocus={this.onFocus}
            onBlur={this.onBlur}
            size={this.props.size}
            style={this.props.size ? { minHeight: `${(this.props.size * 1.5) + 0.5}em` } : undefined}
            disabled={!!this.props.disabled}
          >
            {this.props.children}
          </select>
        </div>
      );
    }

    static optionFor(option: SelectOption): JSX.Element {
      return (
        <option value={option.value} key={option.key}>{option.label}</option>
      );
    }
}

export default Select;
