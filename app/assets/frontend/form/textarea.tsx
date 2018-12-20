import * as React from 'react';
import autobind from '../lib/autobind';
import {FocusableTextInputInterface} from "./input";

interface Props {
  autoFocus?: boolean,
  className?: Option<string>,
  id?: string,
  onBlur?: Option<(value: string) => void>,
  onChange: (newValue: string) => void,
  onFocus?: Option<(value: string) => void>,
  placeholder?: string,
  rows?: number,
  value: string,
  title?: string
}

class Textarea extends React.Component<Props> implements FocusableTextInputInterface {
  input: Option<HTMLTextAreaElement>;

    constructor(props: Props) {
      super(props);
      autobind(this);
    }

    onChange() {
      if (this.input) {
        this.props.onChange(this.input.value);
      }
    }

    onBlur() {
      if (typeof(this.props.onBlur) === 'function' && this.input) {
        this.props.onBlur(this.input.value);
      }
    }

    onFocus() {
      if (typeof(this.props.onFocus) === 'function' && this.input) {
        this.props.onFocus(this.input.value);
      }
    }

    isEmpty() {
      return !this.input || !this.input.value;
    }

    focus() {
      if (this.input) {
        this.input.focus();
      }
    }

    blur() {
      if (this.input) {
        this.input.blur();
      }
    }

    select() {
      if (this.input) {
        this.input.select();
      }
    }

    render() {
      return (
        <textarea
          className={"form-input " + (this.props.className || "")}
          ref={(el) => this.input = el}
          id={this.props.id}
          placeholder={this.props.placeholder}
          value={this.props.value}
          autoFocus={this.props.autoFocus}
          onChange={this.onChange}
          onBlur={this.onBlur}
          onFocus={this.onFocus}
          rows={this.props.rows}
          title={this.props.title}
        />
      );
    }
}

export default Textarea;

