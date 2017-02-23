define(function(require) {
var React = require('react');

return React.createClass({
  propTypes: {
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
            <code className="type-disabled type-s position-absolute position-top-right">1</code>
          </div>
          <div className="column column-expand pls">
            <code className="type-s">
              <span className="type-s type-weak">{"function ("}</span>
              {this.props.userParams.map((param, paramIndex) => (
                <span key={`param${paramIndex}`}>{param.name}<span className="type-weak">, </span></span>
              ))}
              <span className="type-weak">{this.boilerplateLine()}</span>
              <span className="type-weak">{") \u007B"}</span>
            </code>
          </div>
        </div>
      </div>
    );
  }
});

});
