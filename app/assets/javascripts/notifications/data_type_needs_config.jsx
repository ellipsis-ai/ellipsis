define(function(require) {
  var React = require('react');

  return React.createClass({
    displayName: 'NotificationForDataTypeNeedsConfig',
    propTypes: {
      details: React.PropTypes.arrayOf(React.PropTypes.shape({
        kind: React.PropTypes.string.isRequired,
        name: React.PropTypes.string.isRequired,
        onClick: React.PropTypes.func.isRequired
      })).isRequired
    },

    renderDetail: function(detail) {
      return (
        <span>
          <span>The <b>{detail.name}</b> data type needs to be configured.</span>
          <span className="mhxs">
            <button type="button"
                    className="button-raw link button-s"
                    onClick={detail.onClick}>
              Configure it
            </button>
          </span>
        </span>
      );
    },

    render: function() {
      return this.renderDetail(this.props.details[0]);
    }

  });
});
