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
    onGithubPushClick: () => void
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

            <h4>Link to GitHub</h4>

            <GithubOwnerRepoReadonly linked={this.props.linkedGithubRepo}
              onChangeLinkClick={this.props.onChangeGithubLinkClick} />

            <div className="mvl">
              <Button className="mrs" onClick={this.props.onGithubPullClick}>Pull latest version from GitHub…</Button>
              <Button className="mrs" onClick={this.props.onGithubPushClick}>Push current version to GitHub…</Button>
            </div>

          </div>
        </div>
      );
    }
  }

  return GithubActions;
});
