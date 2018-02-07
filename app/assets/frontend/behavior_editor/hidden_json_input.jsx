import * as React from 'react';

const HiddenJsonInput = React.createClass({
  propTypes: {
    value: React.PropTypes.string.isRequired
  },
  render: function() {
    return (
      <input type="hidden" name="dataJson" value={this.props.value}/>
    );
  }
});

export default HiddenJsonInput;
