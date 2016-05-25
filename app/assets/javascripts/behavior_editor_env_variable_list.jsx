define(function(require) {
var React = require('react');

return React.createClass({
  displayName: 'BehaviorEditorEnvVariableList',
  render: function() {
    return (
      <div>
        <h5 className="">
          <span>{this.props.expandEnvVariables ? "▾" : "▸"}</span>
          <span> Current environment variables</span>
        </h5>

        <div className="phs pvxs bg-blue-lightest type-weak border border-blue">
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
      </div>
    );
  }
});

});
