define(function(require) {
  var React = require('react');

  return React.createClass({
    propTypes: {
      url: React.PropTypes.string.isRequired,
      label: React.PropTypes.string
    },

    onAddNewBehavior: function() {
      window.location.href = this.props.url;
    },

    render: function() {
      return (
        <button
          onClick={this.onAddNewBehavior}
          className="button-s">{this.props.label || "Add another"}</button>
      );
    }
  });

});

