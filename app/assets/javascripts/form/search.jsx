define(function(require) {
  var React = require('react'),
    FormInput = require('./input'),
    SVGSearch = require('../svg/search');

  return React.createClass({
    displayName: 'FormSearch',
    propTypes: {
      isSearching: React.PropTypes.bool
    },

    getRemainingProps: function() {
      var props = Object.assign({}, this.props);
      delete props.isSearching;
      return props;
    },

    render: function() {
      return (
        <div className={"position-relative " + (this.props.isSearching ? "pulse-faded" : "")}>
          <div><FormInput {...this.getRemainingProps()} style={{ paddingLeft: "32px" }} /></div>
          <div
            className={
              "position-absolute position-top-left position-z-above align-button mls type-weak"
            }
            style={{ height: "24px" }}
          >
            <SVGSearch />
          </div>
        </div>
      );
    }
  });
});
