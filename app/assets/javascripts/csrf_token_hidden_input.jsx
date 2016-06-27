define(function(require) {

  var React = require('react');

  return React.createClass({
    propTypes: {
      value: React.PropTypes.string.isRequired
    },
    render: function() {
      return (
        <input type="hidden" name="csrfToken" value={this.props.value}/>
      );
    }
  });

});
