import * as React from 'react';

const CsrfTokenHiddenInput = React.createClass({
    propTypes: {
      value: React.PropTypes.string.isRequired
    },
    render: function() {
      return (
        <input type="hidden" name="csrfToken" value={this.props.value}/>
      );
    }
});

export default CsrfTokenHiddenInput;
