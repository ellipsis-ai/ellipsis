// @flow
define(function(require) {
  const React = require('react'),
    Button = require('../../form/button'),
    LinkedGithubRepo = require('../../models/linked_github_repo'),
    autobind = require('../../lib/autobind');

  type Props = {
    linked?: LinkedGithubRepo,
    onChangeLinkClick?: () => void
  }

  class GithubOwnerRepoReadonly extends React.PureComponent<Props> {
    constructor(props) {
      super(props);
      autobind(this);
    }

    render(): React.Node {
      if (this.props.linked) {
        const path = this.props.linked.getOwnerAndRepo();
        const url = this.props.linked.getUrl();
        return (
          <div>
            <span className="display-inline-block align-m type-s">
              <a href={url} target="github" className="type-monospace mrm">{path}</a>
              {this.props.onChangeLinkClick ? (
                <Button className="button-s button-shrink" onClick={this.props.onChangeLinkClick}>Change repo</Button>
              ) : null}
            </span>
          </div>
        );
      } else {
        return null;
      }
    }
  }

  return GithubOwnerRepoReadonly;
});
