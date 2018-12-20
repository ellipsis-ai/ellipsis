import * as React from 'react';

interface Props {
  value: string
}

class CsrfTokenHiddenInput extends React.PureComponent<Props> {
  render() {
    return (
      <input type="hidden" name="csrfToken" value={this.props.value}/>
    );
  }
}

export default CsrfTokenHiddenInput;
