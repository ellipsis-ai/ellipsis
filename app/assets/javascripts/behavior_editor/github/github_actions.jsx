// @flow
define(function(require) {
  const React = require('react'),
    Button = require('../../form/button'),
    GithubOwnerRepoReadonly = require('./github_owner_repo_readonly'),
    LinkedGithubRepo = require('../../models/linked_github_repo'),
    autobind = require('../../lib/autobind');

  type Props = {
    linkedGithubRepo: LinkedGithubRepo,
    onChangeGithubLinkClick: () => void,
    onGithubPullClick: () => void,
    onGithubPushClick: () => void,
    isModified: boolean
  };

  class GithubActions extends React.Component<Props> {
    props: Props;

    constructor(props) {
      super(props);
      autobind(this);
    }

    render() {
      return (
        <div>
          <hr className="mvxxl rule-subtle"/>

          <div className="container container-narrow">

            <h4>Sync with GitHub</h4>

            <div>
              <span className="display-inline-block align-m mrm">
                <span className="type-label mrs">Repository:</span>
                <GithubOwnerRepoReadonly linked={this.props.linkedGithubRepo} />
              </span>
              <Button className="button-s button-shrink" onClick={this.props.onChangeGithubLinkClick}>Change repo…</Button>
            </div>

            <div className="mvl">
              <Button className="mrs"
                onClick={this.props.onGithubPullClick}
                disabled={this.props.isModified}
              >Pull latest version from GitHub…</Button>
              <Button className="mrs"
                onClick={this.props.onGithubPushClick}
                disabled={this.props.isModified}
              >Push current version to GitHub…</Button>
            </div>

            {this.props.isModified ? (
              <div className="fade-in type-bold">
                Save or undo changes to this skill before syncing with GitHub.
              </div>
            ) : null}

          </div>
        </div>
      );
    }
  }

  return GithubActions;
});
