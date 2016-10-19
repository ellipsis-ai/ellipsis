define(function(require) {
  var React = require('react');

  return React.createClass({
    displayName: 'NotificationForDataTypeNeedsConfig',
    propTypes: {
      details: React.PropTypes.arrayOf(React.PropTypes.shape({
        kind: React.PropTypes.string.isRequired,
        name: React.PropTypes.string.isRequired
      })).isRequired
    },

    renderDetail: function(detail) {
      return (
        <span>
          <span>The <b>{detail.name}</b> data type needs to be configured.</span>
          <span className="mhxs">
            <a href={detail.link}>Configure it</a>
          </span>
        </span>
      );
    },

    render: function() {
      return this.renderDetail(this.props.details[0]);
    }

  });
});
