define(function(require) {
var React = require('react'),
  BehaviorEditorMixin = require('./behavior_editor_mixin'),
  HelpButton = require('../help/help_button');

return React.createClass({
  mixins: [BehaviorEditorMixin],
  propTypes: {
    shouldExpandParams: React.PropTypes.bool,
    helpVisible: React.PropTypes.bool,
    onToggleHelp: React.PropTypes.func.isRequired,
    userParams: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
    systemParams: React.PropTypes.arrayOf(React.PropTypes.string).isRequired
  },
  boilerplateLineNumber: function() {
    return this.props.shouldExpandParams ? this.props.userParams.length + 2 : 1;
  },
  boilerplateLine: function() {
    var systemParamString = this.props.systemParams.join(", ");
    if (this.props.shouldExpandParams) {
      return (
        <span className="plm">
          <span>{systemParamString} </span>
        </span>
      );
    } else {
      return (
        <span>
          <span>function(</span>
          <span>{systemParamString}</span>
          <span>{") { "}</span>
        </span>
      );
    }
  },
  render: function() {
    return (
      <div>

        <div className={this.props.shouldExpandParams ? "" : "display-none"}>
          <div className="columns columns-elastic">
            <div className="column column-shrink plxxxl prn align-r position-relative">
              <code className="type-disabled type-s position-absolute position-top-right prxs">1</code>
            </div>
            <div className="column column-expand plxs">
              <code className="type-weak type-s">{"function ("}</code>
            </div>
          </div>
        </div>

        {this.props.userParams.map(function(param, paramIndex) {
          return (
            <div key={'param' + paramIndex} className="columns columns-elastic">
              <div className="column column-shrink plxxxl prn align-r position-relative">
                <code className="type-disabled type-s position-absolute position-top-right prxs">{paramIndex + 2}</code>
              </div>
              <div className="column column-expand pll">
                <div className="type-monospace type-s">{param.name},</div>
              </div>
            </div>
          );
        }, this)}

        <div className="columns">
          <div className="column">
            <div className="columns columns-elastic">
              <div className="column column-shrink plxxxl prn align-r position-relative">
                <code className="type-disabled type-s position-absolute position-top-right prxs">{this.boilerplateLineNumber()}</code>
              </div>
              <div className="column plxs">
                <code className="type-weak type-s">{this.boilerplateLine()}</code>
                <HelpButton onClick={this.props.onToggleHelp} toggled={this.props.helpVisible} />
              </div>
            </div>
          </div>
        </div>

        <div className={this.props.shouldExpandParams ? "" : "display-none"}>
          <div className="columns columns-elastic pbs">
            <div className="column column-shrink plxxxl prn align-r position-relative">
              <code className="type-disabled type-s position-absolute position-top-right prxs">{(this.boilerplateLineNumber() + 1)}</code>
            </div>
            <div className="column column-expand plxs">
              <code className="type-weak type-s">{") {"}</code>
            </div>
          </div>
        </div>

      </div>
    );
  }
});

});
