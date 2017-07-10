define(function(require) {
  const React = require('react'),
    Button = require('../form/button');

  const DataTypeDataSummary = React.createClass({
    displayName: 'DataTypeDataSummary',
    propTypes: {
      // string: React.PropTypes.string.isRequired,
      // callback: React.PropTypes.func.isRequired,
      // children: React.PropTypes.node.isRequired
    },

    render: function() {
      return (
        <div>
          <p className="type-s type-weak">
            No items stored yet.
          </p>

          <div>
            <Button className="button-s mrs mbs" disabled={true}>
              Browse data
            </Button>

            <Button className="button-s mrs mbs">
              Add items
            </Button>
          </div>
        </div>
      );
    }
  });

  return DataTypeDataSummary;
});
