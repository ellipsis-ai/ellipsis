define(function(require) {
var React = require('react'),
  CsrfTokenHiddenInput = require('./csrf_token_hidden_input');

return React.createClass({
  onClick: function(event) {
    if (!window.confirm("Are you sure you want to delete this behavior?")) {
      event.preventDefault();
      event.target.blur();
    }
  },

  render: function() {
    if (this.props.behaviorId !== undefined) {
      return (
        <form action="/delete_behavior" method="POST">
          <CsrfTokenHiddenInput value={this.props.csrfToken} />
          <input type="hidden" name="behaviorId" value={this.props.behaviorId} />
          <button type="submit" className="button-warning" onClick={this.onClick}>
            Delete behavior
          </button>
        </form>
      );
    } else {
      return null;
    }
  }
});

});