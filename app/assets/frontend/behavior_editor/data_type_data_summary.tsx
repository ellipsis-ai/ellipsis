import * as React from 'react';
import Button from '../form/button';
import autobind from "../lib/autobind";

interface Props {
  isModified: boolean,
  isValid: boolean,
  onAddItems: () => void,
  onBrowse: () => void
}

class DataTypeDataSummary extends React.PureComponent<Props> {
    constructor(props: Props) {
      super(props);
      autobind(this);
    }

    isInvalid(): boolean {
      return this.props.isModified || !this.props.isValid;
    }

    getErrorText(): Option<string> {
      if (this.props.isModified && !this.props.isValid) {
        return "Save changes and correct errors to add data";
      } else if (this.props.isModified) {
        return "Save changes to add data";
      } else if (!this.props.isValid) {
        return "Correct errors to add data";
      } else {
        return null;
      }
    }

    renderErrorMessage() {
      const errorText = this.getErrorText();
      return errorText ? (
        <span className="display-inline-block align-m mbs fade-in type-s type-pink type-italic">
          — {errorText}
        </span>
      ) : null;
    }

    render() {
      return (
        <div>
          {/* TODO: Disabled until we can report a data count
          <p className="type-s type-weak">
            No items stored yet.
          </p>
          */}

          <div>
            <Button className="button-s mrs mbs" disabled={this.isInvalid()} onClick={this.props.onBrowse}>
              Browse data
            </Button>

            <Button className="button-s mrs mbs" disabled={this.isInvalid()} onClick={this.props.onAddItems}>
              Add items
            </Button>

            {this.renderErrorMessage()}
          </div>
        </div>
      );
    }
}

export default DataTypeDataSummary;
