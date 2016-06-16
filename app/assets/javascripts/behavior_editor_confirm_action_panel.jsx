define(function(require) {
var React = require('react');

return React.createClass({
  propTypes: {
    onCancelClick: React.PropTypes.func.isRequired,
    onConfirmClick: React.PropTypes.func.isRequired
  },
  render: function() {
    return (
      <div className="box-action">
        <div className="container phn">
          <div>
            {React.Children.map(this.props.children, function(child) { return child; })}
          </div>
          <div className="mtl">
            <button type="button" className="button-primary mrs" onClick={this.props.onConfirmClick}>Okay</button>
            <button type="button" onClick={this.props.onCancelClick}>Cancel</button>
          </div>
        </div>
      </div>
    );
  }
});

});
