define(function(require) {
var React = require('react'),
  HelpButton = require('../help/help_button');

return React.createClass({
  propTypes: {
    helpVisible: React.PropTypes.bool,
    onToggleHelp: React.PropTypes.func.isRequired,
    userParams: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
    systemParams: React.PropTypes.arrayOf(React.PropTypes.string).isRequired
  },
  boilerplateLine: function() {
    return this.props.systemParams.join(", ");
  },
  render: function() {
    return (
      <div className="pbxs">
        <div className="columns columns-elastic">
          <div className="column column-shrink plxxxl prn align-r position-relative">
            <code className="type-disabled type-s position-absolute position-top-right prxs">1</code>
          </div>
          <div className="column column-expand plxs">
            <code className="type-s">
              <span className="type-s type-weak">{"function ("}</span>
              {this.props.userParams.map((param, paramIndex) => (
                <span key={`param${paramIndex}`}>{param.name}<span className="type-weak">, </span></span>
              ))}
              <span className="type-weak mrxs">{this.boilerplateLine()}</span>
              <HelpButton onClick={this.props.onToggleHelp} toggled={this.props.helpVisible} />
              <span className="type-weak mlxs">{") {"}</span>
            </code>
          </div>
        </div>
      </div>
    );
  }
});

});
