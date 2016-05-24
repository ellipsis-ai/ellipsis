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
              <div>
                <span>Call <code>onSuccess</code> to return a response, e.g. </span>
                <span className="pas pvxs bg-blue-lightest type-weak border border-blue">
                  <code>{'onSuccess("It worked!");'}</code>
                </span>
              </div>
            </li>

            <li className="mbs">
              <div>
                <span>Call <code>onError</code> to return an error message, e.g. </span>
                <span className="pas pvxs bg-blue-lightest type-weak border border-blue">
                  <code>onError("Something went wrong.");</code>
                </span>
              </div>
            </li>

            <li className="mbs">
              <div>The <code>ellipsis</code> object reveals useful properties and environment variables.</div>
              <div className="pas pvxs bg-blue-lightest type-weak border border-blue">
                <code>{'ellipsis.apiBaseUrl  //'}</code> <em>the Ellipsis base URL with no trailing slash</em><br />
                <code>{'ellipsis.token       //'}</code> <em>one-time token to authenticate to storage</em><br />
                <code>{'ellipsis.env: {      //'}</code> <em>object with keys for the current environment variables</em><br />
                <code>{'  AWS_ACCESS_KEY,'}</code><br />
                <code>{'  AWS_SECRET_KEY'}</code><br />
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
