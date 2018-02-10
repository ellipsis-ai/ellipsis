// @flow
import * as React from 'react';
import SVGWarning from '../../svg/warning';

type Props = {
  error: string
};

class GithubErrorNotification extends React.PureComponent<Props> {
    props: Props;

    getErrorText() {
      return this.props.error.trim().replace(/\n/g, "; ");
    }

    render() {
      if (this.props.error) {
        return (
          <div className="fade-in type-pink type-bold type-italic">
            <span style={{ height: 24 }} className="display-inline-block mrs align-m"><SVGWarning /></span>
            <span>{this.getErrorText()}</span>
          </div>
        );
      } else {
        return null;
      }
    }
}

export default GithubErrorNotification;

