define(function(require) {
var React = require('react'),
  BehaviorEditorMixin = require('./behavior_editor_mixin'),
  HelpButton = require('../help/help_button');

return React.createClass({
  mixins: [BehaviorEditorMixin],
  propTypes: {
    helpVisible: React.PropTypes.bool,
    onToggleHelp: React.PropTypes.func.isRequired,
    userParams: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
    systemParams: React.PropTypes.arrayOf(React.PropTypes.string).isRequired
  },
  boilerplateLineNumber: function() {
    return 1;
  },
  boilerplateLine: function() {
    return this.props.systemParams.join(", ");
  },
  render: function() {
    return (
      <div className="mbxs">
        <div className="columns columns-elastic">
          <div className="column column-shrink plxxxl prn align-r position-relative">
            <code className="type-disabled type-s position-absolute position-top-right prxs">1</code>
          </div>
          <div className="column column-expand plxs">
            <code className="type-s">
              <span className="type-s type-weak">{"function ("}</span>
              {this.props.userParams.map((param, paramIndex) => (
                <span key={`param${paramIndex}`}>{param.name}, </span>
              ))}
              <span className="type-weak">{this.boilerplateLine()}</span> <HelpButton onClick={this.props.onToggleHelp} toggled={this.props.helpVisible} />
              <span className="type-weak">{") {"}</span>
            </code>
          </div>
        </div>
      </div>
    );
  }
});

});
