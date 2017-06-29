define(function(require) {
  var React = require('react');

  const NotificationForDataTypeUnnamedFields = React.createClass({
    displayName: 'NotificationForDataTypeUnnamedFields',
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
            <span>Each data type field must have a name.</span>
            <button type="button"
              className="button-raw link button-s mhxs"
              onClick={this.props.details[0].onClick}
            >Edit fields on {this.props.details[0].name || "data type"}</button>
          </span>
        );
      } else {
        return (
          <span>
            <span>Each data type field must have a name.</span>
            {this.props.details.map((ea, index) => (
              <span>
                <button key={`detail${index}`}
                  type="button"
                  className="mhxs phxs button-raw link button-s"
                  onClick={ea.onClick}
                >Edit fields on {ea.name || `data type ${index + 1}`}</button>
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

  return NotificationForDataTypeUnnamedFields;
});
