import * as React from 'react';
import ifPresent from '../lib/if_present';

const SectionHeading = React.createClass({
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

export default SectionHeading;
