define(function(require) {
var React = require('react');

return React.createClass({
  displayName: 'BehaviorEditorHiddenJsonInput',
  render: function() {
    return (
      <input type="hidden" name="dataJson" value={this.props.value}/>
    );
  }
});

});
