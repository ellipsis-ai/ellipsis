define(function(require) {
  const React = require('react'),
    Button = require('../form/button');

  class DataTypeDataSummary extends React.Component {
    canAddData() {
      return !this.props.isModified && this.props.isValid;
    }

    getErrorText() {
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
            {/*<Button className="button-s mrs mbs" disabled={true} onClick={this.props.onBrowse}>
              Browse data
            </Button> */}

            <Button className="button-s mrs mbs" disabled={!this.canAddData()} onClick={this.props.onAddItems}>
              Add items
            </Button>

            {this.renderErrorMessage()}
          </div>
        </div>
      );
    }
  }

  DataTypeDataSummary.propTypes = {
    isModified: React.PropTypes.bool.isRequired,
    isValid: React.PropTypes.bool.isRequired,
    onAddItems: React.PropTypes.func.isRequired,
    onBrowse: React.PropTypes.func.isRequired
  };

  return DataTypeDataSummary;
});
