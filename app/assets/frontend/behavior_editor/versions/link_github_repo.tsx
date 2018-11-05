import * as React from 'react';
import Button from '../../form/button';
import LinkedGithubRepo from '../../models/linked_github_repo';
import FormInput from '../../form/input';
import autobind from '../../lib/autobind';
import DynamicLabelButton, {DynamicLabelButtonLabel} from "../../form/dynamic_label_button";
import {EXAMPLE_GITHUB_SKILL_URL} from "../../lib/constants";

type Props = {
  linked: Option<LinkedGithubRepo>,
  onDoneClick: () => void,
  onLinkGithubRepo: (owner: string, repo: string, branch: Option<string>, callback: () => void) => void,
  isLinking?: boolean
  linkButtonLabels?: Array<DynamicLabelButtonLabel>
  error?: Option<string>
};

type State = {
  repoUrl: string,
  invalidUrl: boolean
};

type GithubRepoMatch = {
  owner: Option<string>,
  repo: Option<string>
}

class LinkGithubRepo extends React.Component<Props, State> {
  repoUrlInput: Option<FormInput>;
  timerId: Option<number>;

  constructor(props: Props) {
    super(props);
    autobind(this);
    this.state = this.getDefaultState();
    this.timerId = null;
  }

  getDefaultState(): State {
    return {
      repoUrl: this.props.linked ? this.props.linked.getUrl() : "",
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
    if (this.timerId) {
      clearTimeout(this.timerId);
    }
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

  renderOwnerAndRepo() {
    const match = this.matchOwnerAndRepoFromUrl(this.getRepoUrl());
    if (match.owner && match.repo) {
      return (
        <p>
          <span className="mrxs">Use the </span>
          <span className="border type-monospace type-s mrxs phxs">{match.repo}</span>
          <span className="mrxs"> repo owned by </span>
          <span className="border type-monospace type-s mrxs phxs">{match.owner}</span>
        </p>
      );
    } else {
      return (
        <p>Copy and paste an existing GitHub repository URL.</p>
      );
    }
  }

  getLinkButtonLabels(): Array<DynamicLabelButtonLabel> {
    return this.props.linkButtonLabels || [{
      text: "Link",
      displayWhen: true
    }]
  }

  renderError() {
    if (this.state.invalidUrl) {
      return (
        <span className="align-button">
          <span className="type-pink type-bold type-italic">— Invalid URL: </span>
          <span className="type-s">use a valid GitHub URL, e.g. </span>
          <span className="box-code-example display-nowrap">{EXAMPLE_GITHUB_SKILL_URL}</span>
        </span>
      );
    } else if (this.props.error) {
      return (
        <span className="type-pink type-bold type-italic align-button">— {this.props.error}</span>
      );
    } else {
      return null;
    }
  }

  render() {
    const match = this.matchOwnerAndRepoFromUrl(this.getRepoUrl());
    const validRepo = match && match.repo && match.owner;
    return (
      <div>
        <div>
          {this.renderOwnerAndRepo()}
        </div>
        <div className="columns columns-elastic">
          <div className="column column-shrink display-nowrap">
            <div className="align-button type-label mrs">Repository URL:</div>
          </div>
          <div className="column column-expand">
            <FormInput
              className="form-input-borderless type-monospace"
              ref={(el) => this.repoUrlInput = el}
              onChange={this.onRepoUrlChange}
              placeholder={`e.g. ${EXAMPLE_GITHUB_SKILL_URL}`}
              value={this.getRepoUrl()}
              disabled={this.props.isLinking}
            />
          </div>
        </div>
        <div className="mtl">
          <DynamicLabelButton
            className="button-primary mrs"
            onClick={this.onLinkClick}
            disabledWhen={!validRepo || !this.isLinkModified() || this.props.isLinking}
            labels={this.getLinkButtonLabels()}
          />
          <Button
            className="mrs"
            onClick={this.onCancelClick}
            disabled={this.props.isLinking}
          >
            Cancel
          </Button>
          {this.renderError()}
        </div>
      </div>
    );
  }
}

export default LinkGithubRepo;

