define(function(require) {
  var React = require('react'),
    FormInput = require('./input');

  return React.createClass({
    displayName: 'FormSearch',
    propTypes: {
      className: React.PropTypes.string,
      isSearching: React.PropTypes.bool
    },

    getRemainingProps: function() {
      var props = Object.assign({}, this.props);
      delete props.className;
      return props;
    },

    render: function() {
      return (
        <div className="position-relative">
          <div><FormInput
            className={"plxxl " + (this.props.className || "")}
            {...this.getRemainingProps()}
          /></div>
          <div className="position-absolute position-top-left position-z-above align-button mls">
            {this.props.isSearching ? (
              <span className="pulse opacity-75">⏳</span>
            ) : (
              <span>🔎</span>
            )}
          </div>
        </div>
      );
    }
  });
});
