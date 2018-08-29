import * as React from 'react';
import FormInput from '../form/input';
import DayOfMonth from '../models/day_of_month';
import autobind from "../lib/autobind";

interface Props {
  value: Option<number>
  onChange: (newValue: Option<number>) => void
}

class DayOfMonthInput extends React.Component<Props> {
    constructor(props: Props) {
      super(props);
      autobind(this);
    }

    onChange(newValue: string): void {
      this.props.onChange(DayOfMonth.fromString(newValue).value);
    }

    getTextValue(): string {
      return new DayOfMonth(this.props.value).toString();
    }

    getOrdinalSuffix(): string {
      return new DayOfMonth(this.props.value).ordinalSuffix();
    }

    render() {
      return (
        <span>
          <FormInput
            className="width-2 form-input-borderless align-c"
            value={this.getTextValue()}
            onChange={this.onChange}
          />
          <span className="align-button type-label type-monospace">{this.getOrdinalSuffix()}</span>
        </span>
      );
    }
}

export default DayOfMonthInput;
