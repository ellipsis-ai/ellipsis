define(function(require) {
  var React = require('react');

  return React.createClass({
    propTypes: {
      url: React.PropTypes.string.isRequired,
      label: React.PropTypes.string
    },

    render: function() {
      return (
        <a
          href={this.props.url}
          className="button button-s button-shrink">{this.props.label || "Add another"}</a>
      );
    }
  });

});

