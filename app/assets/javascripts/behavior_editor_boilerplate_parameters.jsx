define(function(require) {
  var React = require('react');

  return React.createClass({
    displayName: 'BehaviorEditorBoilerplateParameters',
    render: function() {
      return (
        <code className="type-weak type-s">onSuccess,<br />onError,<br />ellipsis</code>
      );
    }
  });

});
