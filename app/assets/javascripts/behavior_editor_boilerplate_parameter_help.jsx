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
        <div className="bg-blue-lighter border border-emphasis-left border-blue type-s ptl phl pbxs">
          <p>
            <span>In addition to any parameters you define, your function will receive three parameters from Ellipsis.</span>
          </p>

          <ul>
            <li className="mbs">
              <div>Call <code>onSuccess</code> to return a response to the user.</div>
              <div className="pas pvxs bg-blue-lightest type-weak border border-blue">
                <code>{'onSuccess("It worked!");'}</code>
              </div>
            </li>

            <li className="mbs">
              <div>Call <code>onError</code> to return an error message to the user.</div>
              <div className="pas pvxs bg-blue-lightest type-weak border border-blue">
                <code>onError("Something went wrong.");</code>
              </div>
            </li>

            <li className="mbs">
              <div>Use the <code>ellipsis</code> object to reference external properties or environment variables.</div>
              <div className="pas pvxs bg-blue-lightest type-weak border border-blue">
                <code>{'ellipsis: {'}</code><br />
                <code>{'  apiBaseUrl: '}</code><span className="type-weak">…</span><code> /*</code> <em>the Ellipsis base URL with no trailing slash</em><code> */</code><br />
                <code>{'  token:      '}</code><span className="type-weak">…</span><code> /*</code> <em>one-time token to authenticate to storage</em><code> */</code><br />
                <code>{'  env: {'}</code><br />
                <code>{'    /* '}</code><em>object with keys for the current environment variables</em><code> */</code><br />
                <code>{'    AWS_ACCESS_KEY: '}</code><span className="type-weak">…</span><br />
                <code>{'    AWS_SECRET_KEY: '}</code><span className="type-weak">…</span><br />
                <code>{'  }'}</code><br />
                <code>{'}'}</code>
              </div>
            </li>
          </ul>
        </div>
      </div>
    );
  }
});

});
