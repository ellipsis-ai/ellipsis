define(function(require) {
var React = require('react');

return React.createClass({
  propTypes: {
    lineNumber: React.PropTypes.oneOfType([
      React.PropTypes.number,
      React.PropTypes.string
    ]).isRequired,
    onCodeDelete: React.PropTypes.func.isRequired
  },
  render: function() {
    return (
      <div className="border-left border-bottom border-right border-radius-bottom pvs">
        <div className="columns columns-elastic">
          <div className="column column-shrink plxxxl prn align-r position-relative">
            <code className="type-disabled type-s position-absolute position-top-right prxs">{this.props.lineNumber}</code>
          </div>
          <div className="column column-expand plxs">
            <div className="columns columns-elastic">
              <div className="column column-expand">
                <code className="type-weak type-s">{"}"}</code>
              </div>
              <div className="column column-shrink prxs align-r">
                <button type="button" className="button-s" onClick={this.props.onCodeDelete}>Remove code</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  }
});

});
