define(function(require) {
var React = require('react'),
  ifPresent = require('../lib/if_present');

return React.createClass({
  propTypes: {
    lineNumber: React.PropTypes.oneOfType([
      React.PropTypes.number,
      React.PropTypes.string
    ]).isRequired,
    onCodeDelete: React.PropTypes.func
  },
  render: function() {
    return (
      <div className="border-left border-right border-bottom border-light pvs mbxxl">
        <div className="columns columns-elastic">
          <div className="column column-shrink plxxxl prn align-r position-relative">
            <code className="type-disabled type-s position-absolute position-top-right">{this.props.lineNumber}</code>
          </div>
          <div className="column column-expand pls">
            <div className="columns columns-elastic">
              <div className="column column-expand">
                <code className="type-weak type-s">{"}"}</code>
              </div>
              <div className="column column-shrink prs align-r">
                {ifPresent(this.props.onCodeDelete, (onCodeDelete) => (
                  <button type="button" className="button-s" onClick={onCodeDelete}>Remove code</button>
                ))}
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  }
});

});
