import * as React from 'react';
import autobind from '../lib/autobind';

type Props = {
  checked: boolean,
  onChange: (isChecked: boolean, value?: string) => void,
  onEnterKey?: () => void,
  className?: Option<string>,
  label?: any,
  title?: string,
  name?: string,
  useButtonStyle?: boolean,
  value?: string,
  disabledWhen?: boolean
}

class Checkbox extends React.PureComponent<Props> {
  input: Option<HTMLInputElement>;

  constructor(props: Props) {
    super(props);
    autobind(this);
  }

  onChange() {
    const isChecked = Boolean(this.input && this.input.checked);
    this.props.onChange(isChecked, this.props.value);
  }

  handleEnterKey(event: React.KeyboardEvent<HTMLInputElement>) {
    if (event.which === 13) {
      event.preventDefault();
      if (typeof this.props.onEnterKey === 'function') {
        this.props.onEnterKey();
      }
    }
  }

  focus() {
    if (this.input) {
      this.input.focus();
    }
  }

  getClassName() {
    const classNames: Array<String> = [];
    if (this.props.useButtonStyle) {
      if (this.props.checked) {
        classNames.push("checkbox-button checkbox-button-s checkbox-button-checked");
      } else {
        classNames.push("checkbox-button checkbox-button-s");
      }
    }
    if (this.props.className) {
      classNames.push(this.props.className);
    }
    return classNames.join(" ");
  }

  render() {
    return (
      <label className={this.getClassName()} title={this.props.title}>
        <input
          ref={(el) => this.input = el}
          type="checkbox"
          className={this.props.label ? "man mrs" : "man"}
          checked={this.props.checked}
          onChange={this.onChange}
          onKeyPress={this.handleEnterKey}
          name={this.props.name}
          value={this.props.value || "on"}
          disabled={this.props.disabledWhen}
        />
        <span>{this.props.label}</span>
      </label>
    );
  }
}

export default Checkbox;
