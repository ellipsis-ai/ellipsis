// @flow
define(function(require) {
  const React = require('react'),
    Button = require('../../form/button'),
    BehaviorGroup = require('../../models/behavior_group'),
    LinkedGithubRepo = require('../../models/linked_github_repo'),
    FormInput = require('../../form/input'),
    autobind = require('../../lib/autobind');

  type Props = {
    group: BehaviorGroup,
    linked?: LinkedGithubRepo,
    onDoneClick: () => void,
    onLinkGithubRepo: () => void,
    csrfToken: string
  }

  class GithubLinkPanel extends React.Component<Props> {
    constructor(props) {
      super(props);
      autobind(this);
      this.state = {
        owner: this.props.linked ? this.props.linked.getOwner() : "",
        repo: this.props.linked ? this.props.linked.getRepo(): ""
      };
    }

    isLinkModified(): boolean {
      return !this.props.linked || this.props.linked.getOwner() !== this.state.owner || this.props.linked.getRepo() !== this.state.repo;
    }

    getOwner(): string {
      return this.state.owner || "";
    }

    onOwnerChange(owner: string): void {
      this.setState({
        owner: owner
      });
    }

    getRepo(): string {
      return this.state.repo || "";
    }

    onRepoChange(repo: string): void {
      this.setState({
        repo: repo
      });
    }

    onLinkClick(): void {
      this.props.onLinkGithubRepo(this.getOwner(), this.getRepo(), () => this.props.onDoneClick());
    }

    renderContent(): React.Node {
      return (
        <div>
          <div className="columns">
            <div className="column column-one-quarter">
              <span className="display-inline-block align-m type-s type-weak mrm">Owner:</span>
              <FormInput
                className="form-input-borderless type-monospace type-s width-15 mrm"
                placeholder="e.g. github"
                onChange={this.onOwnerChange}
                value={this.getOwner()}
              />
            </div>
            <div className="column column-one-quarter">
              <span className="display-inline-block align-m type-s type-weak mrm">Repo:</span>
              <FormInput
                className="form-input-borderless type-monospace type-s width-15 mrm"
                placeholder="e.g. octocat"
                onChange={this.onRepoChange}
                value={this.getRepo()}
              />
            </div>
          </div>
          <div className="mtl">
            <Button
              onClick={this.onLinkClick}
              disabled={!this.getRepo() || !this.getOwner() || !this.isLinkModified() }
            >
              Link
            </Button>
            <Button
              className="mls"
              onClick={this.props.onDoneClick}
            >
              Cancel
            </Button>
          </div>
        </div>
      );
    }

    render(): React.Node {
      return (
        <div className="box-action phn">
          <div className="container">
            <div className="columns">
              <div className="column column-page-sidebar">
                <h4 className="type-weak mtn">Link this skill to a GitHub repo</h4>
              </div>
              <div className="column column-page-main">
                {this.renderContent()}
              </div>
            </div>
          </div>
        </div>
      );
    }
  }

  return GithubLinkPanel;
});
