import * as React from 'react';

const NotificationForDataTypeMissingFields = React.createClass({
    displayName: 'NotificationForDataTypeMissingFields',
    propTypes: {
      details: React.PropTypes.arrayOf(React.PropTypes.shape({
        kind: React.PropTypes.string.isRequired,
        name: React.PropTypes.string,
        onClick: React.PropTypes.func.isRequired
      })).isRequired
    },

    render: function() {
      const count = this.props.details.length;
      if (count === 1) {
        return (
          <span>
            <span>Each data type should have at least one text field.</span>
            <button type="button"
              className="button-raw link button-s mhxs"
              onClick={this.props.details[0].onClick}
            >Add field to {this.props.details[0].name || "data type"}</button>
          </span>
        );
      } else {
        return (
          <span>
            <span>Each data type should have at least one text field.</span>
            {this.props.details.map((ea, index) => (
              <span key={`detail${index}`}>
                <button
                  type="button"
                  className="button-raw link button-s mhxs"
                  onClick={ea.onClick}
                >Add field to {ea.name || `data type ${index + 1}`}</button>
                {index + 1 < this.props.details.length ? (
                  <span className="mhxs type-weak">·</span>
                ) : null}
              </span>
            ))}
          </span>
        );
      }
    }

});

export default NotificationForDataTypeMissingFields;

