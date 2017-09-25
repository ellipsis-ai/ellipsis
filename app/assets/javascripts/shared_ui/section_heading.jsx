define(function(require) {
const React = require('react'),
  ifPresent = require('../lib/if_present');

const SectionHeading = React.createClass({
  displayName: "SectionHeading",
  propTypes: {
    number: React.PropTypes.string,
    children: React.PropTypes.node.isRequired
  },
  render: function() {
    return (
      <h4 className="position-relative mtn mbl">
        {ifPresent(this.props.number, (number) => (
          <span className="box-number bg-blue-medium type-white mrm">{number}</span>
        ))}
        <span>
          {this.props.children}
        </span>
      </h4>
    );
  }
});

return SectionHeading;

});
