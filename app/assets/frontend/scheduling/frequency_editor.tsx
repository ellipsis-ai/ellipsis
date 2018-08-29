import * as React from 'react';
import FormInput from '../form/input';
import OptionalInt from '../models/optional_int';
import {RecurrenceEditorProps} from "./recurrence_editor";
import autobind from "../lib/autobind";

type Props = RecurrenceEditorProps & {
  unit: string,
  units: string,
  min: number,
  max: number
}

class FrequencyEditor extends React.Component<Props> {
  constructor(props) {
    super(props);
    autobind(this);
  }

    getFrequency(): Option<number> {
      return this.props.recurrence.frequency;
    }

    getFrequencyType() {
      if (this.props.recurrence.totalTimesToRun === 1) {
        return "In";
      } else {
        return "Repeat every";
      }
    }

    getFrequencySuffix(): string {
      if (this.props.recurrence.totalTimesToRun === 1) {
        return "from now";
      } else {
        return "";
      }
    }

    getTextValue(): string {
      return new OptionalInt(this.getFrequency()).toString();
    }

    getUnit(): string {
      return this.getFrequency() === 1 ? this.props.unit : this.props.units;
    }

    onChange(newValue: string): void {
      const newFrequency = OptionalInt.fromString(newValue).valueWithinRange(this.props.min, this.props.max);
      this.props.onChange(this.props.recurrence.clone({
        frequency: newFrequency
      }));
    }

    render() {
      return (
        <div>
          <span className="align-button mrm type-s">{this.getFrequencyType()}</span>
          <FormInput
            className="width-5 form-input-borderless align-c"
            value={this.getTextValue()}
            onChange={this.onChange}
          />
          <span className="align-button mlm type-s">{this.getUnit()} {this.getFrequencySuffix()}</span>
        </div>
      );
    }
}

export default FrequencyEditor;
