define(function(require) {

  var React = require('react');

  return React.createClass({
    displayName: 'CsrfTokenHiddenInput',
    render: function() {
      return (
        <input type="hidden" name="csrfToken" value={this.props.value}/>
      );
    }
  });

});
