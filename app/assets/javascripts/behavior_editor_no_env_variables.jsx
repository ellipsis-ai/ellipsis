if (typeof define !== 'function') { var define = require('amdefine')(module); }
define(function(require) {
var React = require('react');

return React.createClass({
  displayName: 'BehaviorEditorNoEnvVariables',
  render: function() {
    return (
      <div>
        <h5>Current environment variables</h5>
        <div className="phs pvxs bg-blue-lightest type-weak border border-blue">
          <p className="man">There are no environment variables configured.</p>
        </div>
      </div>
    );
  }
});

});
