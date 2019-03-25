import * as React from 'react';
import FormInput from '../form/input';
import Minute from '../models/minute';
import autobind from "../lib/autobind";

interface Props {
  value: Option<number>,
  onChange: (newValue: Option<number>) => void
}

class MinuteInput extends React.Component<Props> {
    constructor(props: Props) {
      super(props);
      autobind(this);
    }

    getTextValue() {
      return new Minute(this.props.value).toString();
    }

    onChange(newValue: string) {
      this.props.onChange(Minute.fromString(newValue).value);
    }

    render() {
      return (
        <span>
          <span className="align-button">:</span>
          <FormInput
            className="width-2 form-input-borderless align-c"
            value={this.getTextValue()}
            onChange={this.onChange}
          />
        </span>
      );
    }
}

export default MinuteInput;
