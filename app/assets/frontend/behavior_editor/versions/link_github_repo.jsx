// @flow
import * as React from 'react';
import Button from '../../form/button';
import BehaviorGroup from '../../models/behavior_group';
import LinkedGithubRepo from '../../models/linked_github_repo';
import FormInput from '../../form/input';
import autobind from '../../lib/autobind';

type Props = {
  group: BehaviorGroup,
  linked?: LinkedGithubRepo,
  onDoneClick: () => void,
  onLinkGithubRepo: (owner: string, repo: string, branch: ?string, callback: () => void) => void,
  csrfToken: string
};

type State = {
  repoUrl: string,
  invalidUrl: boolean
};

type GithubRepoMatch = {
  owner: ?string,
  repo: ?string
}

class LinkGithubRepo extends React.Component<Props, State> {
    repoUrlInput: ?FormInput;
    timerId: ?number;

    constructor(props: Props) {
      super(props);
      autobind(this);
      this.state = this.getDefaultState();
      this.timerId = null;
    }

    getDefaultState(): State {
      return {
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
      const match = this.matchOwnerAndRepoFromUrl(this.getRepoUrl());
      return !this.props.linked || this.props.linked.getOwner() !== match.owner || this.props.linked.getRepo() !== match.repo;
    }

    getOwner(): string {
      const match = this.matchOwnerAndRepoFromUrl(this.getRepoUrl());
      return match.owner || "";
    }

    getRepo(): string {
      const match = this.matchOwnerAndRepoFromUrl(this.getRepoUrl());
      return match.repo || "";
    }

    getRepoUrl(): string {
      return this.state.repoUrl;
    }

    checkValidUrl(url: string): void {
      const match = this.matchOwnerAndRepoFromUrl(url);
      const owner = match && match.owner;
      const repo = match && match.repo;
      this.setState({
        invalidUrl: Boolean(url && !(owner && repo))
      });
    }

    onRepoUrlChange(url: string): void {
      this.setState({
        repoUrl: url
      });
      clearTimeout(this.timerId);
      this.timerId = setTimeout(() => this.checkValidUrl(url), 1000);
    }

    onLinkClick(): void {
      const match = this.matchOwnerAndRepoFromUrl(this.getRepoUrl());
      if (match.owner && match.repo) {
        this.props.onLinkGithubRepo(match.owner, match.repo, null, () => {
          this.props.onDoneClick();
          this.setState(this.getDefaultState());
        });
      }
    }

    onCancelClick(): void {
      this.props.onDoneClick();
      this.setState(this.getDefaultState());
    }

    matchOwnerAndRepoFromUrl(url: string): GithubRepoMatch {
      const match = url.trim().replace(/\.git$/, "").match(/^(?:(?:https?:\/\/|git@)?github\.com[\/:]?)?([a-z0-9_][a-z0-9_\-]*)\/([a-z0-9\-_.]+)/i);
      return {
        owner: match && match[1],
        repo: match && match[2]
      };
    }

    renderOwnerAndRepo(): React.Node {
      const match = this.matchOwnerAndRepoFromUrl(this.getRepoUrl());
      if (match.owner && match.repo) {
        return (
          <p>
            <span className="mrxs">Link to the </span>
            <span className="border type-monospace type-s mrxs phxs">{match.repo}</span>
            <span className="mrxs"> repo owned by </span>
            <span className="border type-monospace type-s mrxs phxs">{match.owner}</span>
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
      const match = this.matchOwnerAndRepoFromUrl(this.getRepoUrl());
      const validRepo = match && match.repo && match.owner;
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
              disabled={!validRepo || !this.isLinkModified() }
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

export default LinkGithubRepo;

