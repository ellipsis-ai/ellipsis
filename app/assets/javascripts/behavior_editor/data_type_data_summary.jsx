define(function(require) {
  const React = require('react'),
    Button = require('../form/button');

  const DataTypeDataSummary = React.createClass({
    displayName: 'DataTypeDataSummary',
    propTypes: {
      isModified: React.PropTypes.bool.isRequired,
      onAddItems: React.PropTypes.func.isRequired,
      onBrowse: React.PropTypes.func.isRequired
    },

    render: function() {
      return (
        <div>
          <p className="type-s type-weak">
            No items stored yet.
          </p>

          <div>
            <Button className="button-s mrs mbs" disabled={true} onClick={this.props.onBrowse}>
              Browse data
            </Button>

            <Button className="button-s mrs mbs" disabled={this.props.isModified} onClick={this.props.onAddItems}>
              Add items
            </Button>
            {this.props.isModified ? (
              <span className="display-inline-block align-m mbs fade-in type-s type-pink type-italic">
                — Save changes to add data
              </span>
            ) : null}
          </div>
        </div>
      );
    }
  });

  return DataTypeDataSummary;
});
