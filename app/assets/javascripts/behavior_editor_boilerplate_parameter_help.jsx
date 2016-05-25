define(function(require) {
var React = require('react');
var BehaviorEditorMixin = require('./behavior_editor_mixin');

return React.createClass({
  displayName: 'BehaviorEditorBoilerplateParameterHelp',
  mixins: [BehaviorEditorMixin],
  onExpandToggle: function() {
    this.refs.button.blur();
    this.props.onExpandToggle();
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

          <button type="button"
            ref="button"
            className="button-none pan mbxs display-limit-width"
            onClick={this.onExpandToggle}
          >
            <h5 className="">
              <span>{this.props.expandEnvVariables ? "▾" : "▸"}</span>
              <span> Current environment variables</span>
            </h5>

            <div className="pas pvxs bg-blue-lightest type-weak border border-blue">
              <div
                className="display display-ellipsis"
                style={{
                  maxHeight: (this.props.expandEnvVariables ?
                    ((this.props.envVariableNames.length + 3) * 1.5) + 'em' :
                    '1.5em')
                }}
              >
                <code className="prs">{'ellipsis.env: {'}</code>
                {this.props.envVariableNames.map(function(name, index) {
                  return (
                    <div
                      key={"envVar" + index}
                      className={this.props.expandEnvVariables ? "pll" : "display-inline"}
                    >
                      <code>{name + ': '}</code>
                      <span
                        className={"bg-dark-translucent " + (this.props.expandEnvVariables ? "plxxxl" : "plm")}
                        title="For security, environment variable values are not displayed."></span>
                      <code>
                        {index < this.props.envVariableNames.length - 1 ? ", " : " "}
                      </code>
                    </div>
                  );
                }, this)}
                <code>{'}'}</code>
              </div>
            </div>
          </button>
        </div>
      </div>
    );
  }
});

});
