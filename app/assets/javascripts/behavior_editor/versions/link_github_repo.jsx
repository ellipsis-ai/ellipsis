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
    onLinkGithubRepo: (string, string, () => void) => void,
    csrfToken: string
  };

  type State = {
    owner: string,
    repo: string,
    repoUrl: string,
    invalidUrl: boolean
  };

  class LinkGithubRepo extends React.Component<Props, State> {
    props: Props;
    state: State;
    repoUrlInput: ?FormInput;
    timerId: ?number;

    constructor(props) {
      super(props);
      autobind(this);
      this.state = this.getDefaultState();
      this.timerId = null;
    }

    getDefaultState(): State {
      return {
        owner: this.props.linked ? this.props.linked.getOwner() : "",
        repo: this.props.linked ? this.props.linked.getRepo() : "",
        repoUrl: this.props.linked ? this.props.linked.getUrl()  : "",
        invalidUrl: false
      };
    }

    focus(): void {
      if (this.repoUrlInput) {
        this.repoUrlInput.focus();
      }
    }

    isLinkModified(): boolean {
      return !this.props.linked || this.props.linked.getOwner() !== this.state.owner || this.props.linked.getRepo() !== this.state.repo;
    }

    getOwner(): string {
      return this.state.owner;
    }

    getRepo(): string {
      return this.state.repo;
    }

    getRepoUrl(): string {
      return this.state.repoUrl;
    }

    checkValidUrl(url: string, owner: string, repo: string): void {
      this.setState({
        invalidUrl: url && !(owner && repo)
      });
    }

    onRepoUrlChange(url: string): void {
      const match = this.matchOwnerAndRepoFromUrl(url);
      const owner = match ? match[1] : "";
      const repo = match ? match[2] : "";
      this.setState({
        repoUrl: url,
        owner: owner,
        repo: repo
      });
      clearTimeout(this.timerId);
      this.timerId = setTimeout(() => this.checkValidUrl(url, owner, repo), 1000);
    }

    onLinkClick(): void {
      this.props.onLinkGithubRepo(this.getOwner(), this.getRepo(), () => {
        this.props.onDoneClick();
        this.setState(this.getDefaultState());
      });
    }

    onCancelClick(): void {
      this.props.onDoneClick();
      this.setState(this.getDefaultState());
    }

    matchOwnerAndRepoFromUrl(url: string): ?Array<string> {
      return url.trim().replace(/\.git$/, "").match(/^(?:https:\/\/github\.com\/|git@github\.com:)([a-z0-9_][a-z0-9_\-]*)\/([a-z0-9\-_.]+)/i);
    }

    renderOwnerAndRepo(): React.Node {
      const owner = this.getOwner();
      const repo = this.getRepo();
      if (owner && repo) {
        return (
          <p>
            <span className="mrxs">Link to the </span>
            <span className="border type-monospace type-s mrxs phxs">{repo}</span>
            <span className="mrxs"> repo owned by </span>
            <span className="border type-monospace type-s mrxs phxs">{owner}</span>
          </p>
        );
      } else if (this.state.invalidUrl) {
        return (
          <p><span className="type-pink type-bold type-italic">Invalid repository — copy and paste a valid GitHub URL</span></p>
        );
      } else {
        return (
          <p>Copy and paste an existing GitHub repository URL.</p>
        );
      }
    }

    render(): React.Node {
      return (
        <div>
          <div>
            {this.renderOwnerAndRepo()}
          </div>
          <div>
            <div className="align-button type-label mrs">Repository URL:</div>
            <FormInput
              className="form-input-borderless type-monospace width-30"
              ref={(el) => this.repoUrlInput = el}
              onChange={this.onRepoUrlChange}
              placeholder={"e.g. https://github.com/your_company/your_repo"}
              value={this.getRepoUrl()}
            />
          </div>
          <div className="mtl">
            <Button
              className="button-primary"
              onClick={this.onLinkClick}
              disabled={!this.getRepo() || !this.getOwner() || !this.isLinkModified() }
            >
              Link
            </Button>
            <Button
              className="mls"
              onClick={this.onCancelClick}
            >
              Cancel
            </Button>
          </div>
        </div>
      );
    }
  }

  return LinkGithubRepo;
});
