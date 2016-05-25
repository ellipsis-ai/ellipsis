define(function(require) {
var React = require('react');

return React.createClass({
  displayName: 'BehaviorEditorBoilerplateParameterHelp',
  onClick: function() {
    this.props.onClick();
  },
  render: function() {
    return (
      <div className="pvs">
        <div className="bg-blue-lighter border border-emphasis-left border-blue type-s pal">
          <p>
            <span>In addition to any parameters you define, your function will receive three parameters from Ellipsis.</span>
          </p>

          <ul>
            <li className="mbs">
              <div>
                <span>Call <code className="type-bold">onSuccess</code> to return a response, e.g. </span>
                <code className="type-weak">{'onSuccess("It worked!");'}</code>
              </div>
            </li>

            <li className="mbs">
              <div>
                <span>Call <code className="type-bold">onError</code> to return an error message, e.g. </span>
                <code className="type-weak">onError("Something went wrong.");</code>
              </div>
            </li>

            <li className="mbs">
              <div>
                <span>The <code className="type-bold">ellipsis</code> object is required when using the </span>
                <span>default storage library, and it also contains any pre-configured environment variables </span>
                <span>in the <code className="type-bold">env</code> property.</span>
              </div>
            </li>
          </ul>

          <h5 className="mbs">Current environment variables</h5>
          <div className="pas pvxs bg-blue-lightest type-weak border border-blue">
            <code>{'ellipsis.env: {'}</code><br />
            <code>{'  AWS_ACCESS_KEY: '}</code>
            <span title="For security, the value is not displayed">…</span>
            <code>,</code><br />
            <code>{'  AWS_SECRET_KEY: '}</code>
            <span title="For security, the value is not displayed">…</span><br />
            <code>{'}'}</code>
          </div>

        </div>
      </div>
    );
  }
});

});
