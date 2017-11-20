// @flow
define(function(require) {
  const React = require('react'),
    Button = require('../../form/button'),
    LinkedGithubRepo = require('../../models/linked_github_repo'),
    autobind = require('../../lib/autobind');

  type Props = {
    linked?: LinkedGithubRepo,
    onChangeLinkClick: () => void
  }

  class GithubOwnerRepoReadonly extends React.PureComponent<Props> {
    constructor(props) {
      super(props);
      autobind(this);
    }

    getOwner(): string {
      return this.props.linked.getOwner();
    }

    getRepo(): string {
      return this.props.linked.getRepo();
    }

    getOwnerRepo(): string {
      return `${this.getOwner()}/${this.getRepo()}`;
    }

    renderChangeLink(): React.Node {
      if (this.props.onChangeLinkClick) {
        return (
          <Button className="button-s mll" onClick={this.props.onChangeLinkClick}>Change</Button>
        );
      } else {
        return null;
      }
    }

    render(): React.Node {
      if (this.props.linked) {
        return (
          <div>
            <div className="display-inline-block align-m type-s type-weak mrm">Owner/Repo:</div>
            <div className="display-inline-block align-m type-monospace type-s mrm">
              <span>{this.getOwnerRepo()}</span>
            </div>
            {this.renderChangeLink()}
          </div>
        );
      } else {
        return null;
      }
    }
  }

  return GithubOwnerRepoReadonly;
});
