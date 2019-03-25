import * as React from 'react';
import FormInput, {FocusableTextInputInterface} from '../form/input';
import ParamType from '../models/param_type';
import Select from '../form/select';
import autobind from '../lib/autobind';

interface Props {
  name: Option<string>
  value: string
  fieldType?: Option<ParamType>
  onChange?: Option<(newValue: string) => void>
  onEnterKey?: Option<() => void>
  readOnly?: boolean
}

class DefaultStorageAdderField extends React.Component<Props> {
    input: Option<FocusableTextInputInterface>;

    constructor(props: Props) {
      super(props);
      autobind(this);
    }

    onChange(value: string): void {
      if (this.props.onChange) {
        const newValue = this.props.fieldType ? this.props.fieldType.formatValue(value) : value;
        this.props.onChange(newValue);
      }
    }

    focus(): void {
      if (this.input) {
        this.input.focus();
      }
    }

    getPlaceholder(): string | undefined {
      const placeholder = this.props.fieldType && this.props.fieldType.getInputPlaceholder();
      if (placeholder) {
        return placeholder;
      } else {
        return;
      }
    }

    renderInput() {
      const options = this.props.fieldType ? this.props.fieldType.getOptions() : null;
      if (options && !this.props.readOnly) {
        return (
          <div className="align-form-input">
            <Select
              className="form-select-s align-m"
              ref={(input) => this.input = input}
              value={this.props.value}
              onChange={this.onChange}
            >
              {options.map((option) => (
                <option key={option.value} value={option.value}>{option.label}</option>
              ))}
            </Select>
          </div>
        );
      } else {
        return (
          <FormInput
            ref={(input) => this.input = input}
            value={this.props.value}
            onChange={this.onChange}
            onEnterKey={this.props.onEnterKey}
            className="form-input-borderless"
            readOnly={this.props.readOnly}
            placeholder={this.getPlaceholder()}
          />
        );
      }
    }

    render() {
      return (
        <div className="column-row">
          <div className="column column-shrink align-form-input type-s">
            {this.props.name ? (
              <span className="type-monospace">{this.props.name}</span>
            ) : (
              <span className="type-weak type-italic">Unnamed field</span>
            )}
          </div>
          <div className="column column-expand">
            {this.renderInput()}
          </div>
        </div>
      );
    }
}

export default DefaultStorageAdderField;
