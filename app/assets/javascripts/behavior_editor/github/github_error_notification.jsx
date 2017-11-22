// @flow
define(function(require) {
  const React = require('react'),
    SVGWarning = require('../../svg/warning'),
    autobind = require('../../lib/autobind');

  type Props = {
    error: string
  };

  class GithubErrorNotification extends React.Component<Props> {
    props: Props;

    constructor(props) {
      super(props);
      autobind(this);
    }

    render() {
      if (this.props.error) {
        return (
          <div className="display-inline-block align-button mlm">
            <span className="fade-in type-pink type-bold type-italic">
              <span style={{ height: 24 }} className="display-inline-block mrs align-b"><SVGWarning /></span>
              <span>{this.props.error}</span>
            </span>
          </div>
        );
      } else {
        return null;
      }
    }
  }

  return GithubErrorNotification;
});
