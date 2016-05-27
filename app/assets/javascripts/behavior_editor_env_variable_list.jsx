define(function(require) {
var React = require('react');

return React.createClass({
  displayName: 'BehaviorEditorEnvVariableList',
  lineHeight: 1.5,
  isLastIndex: function(index) {
    return index + 1 === this.props.envVariableNames.length;
  },
  maxHeightForVars: function() {
    var height;
    if (this.props.expandEnvVariables) {
      // Add 2 extra lines for opening/closing braces, and 1 more for safety
      height = this.lineHeight * (this.props.envVariableNames.length + 3);
    } else {
      height = this.lineHeight;
    }
    return height + 'em';
  },
  render: function() {
    return (
      <div>
        <h5>
          <span
            className="display-inline-block"
            style={{ width: '0.8em' }}
          >{this.props.expandEnvVariables ? "▾" : "▸"}</span>
          <span> Current environment variables</span>
        </h5>

        <div className="phs pvxs bg-blue-lightest type-weak border border-blue">
          <div
            className="display display-ellipsis"
            style={{ maxHeight: this.maxHeightForVars() }}
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
                    className={
                      "bg-dark-translucent " +
                      (this.props.expandEnvVariables ? "plxxxl" : "plm")
                    }
                    title="For security, environment variable values are not displayed.">
                  </span>
                  <code>{this.isLastIndex(index) ? " " : ", "}</code>
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
