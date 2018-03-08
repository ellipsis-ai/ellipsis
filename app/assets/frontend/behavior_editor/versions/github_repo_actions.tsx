import * as React from 'react';
import autobind from '../../lib/autobind';
import Button from '../../form/button';
import LinkedGithubRepo from "../../models/linked_github_repo";
import GithubOwnerRepoReadonly from '../github/github_owner_repo_readonly';
import BehaviorGroup from "../../models/behavior_group";

type Props = {
  linkedGithubRepo?: LinkedGithubRepo,
  isLinkedToGithub: boolean,
  currentGroupIsModified: boolean,
  onChangeGithubRepo: () => void,
  currentGroup: BehaviorGroup,
  currentSelectedId?: string
};

class GithubRepoActions extends React.Component<Props> {
  props: Props;

  constructor(props: Props) {
    super(props);
    autobind(this);
  }

  getGithubAuthUrl(groupId: string, currentSelectedId: string): string {
    const redirect = jsRoutes.controllers.BehaviorEditorController.edit(groupId, currentSelectedId, true).url;
    return jsRoutes.controllers.SocialAuthController.authenticateGithub(redirect).url;
  }

  onChangeGithubLinkClick(): void {
    this.props.onChangeGithubRepo();
  }

  renderChangeRepoButton() {
    return (
      <Button className="button-s button-shrink" onClick={this.onChangeGithubLinkClick}>
        {this.props.linkedGithubRepo ? "Change repo…" : "Link GitHub repo…"}
      </Button>
    );
  }

  renderGithubAuth() {
    const groupId = this.props.currentGroup.id;
    const selectedId = this.props.currentSelectedId;
    if (groupId && selectedId) {
      return (
        <span className="type-s">
          <a href={this.getGithubAuthUrl(groupId, selectedId)}>
            <img height="24" src="/assets/images/logos/GitHub-Mark-64px.png" className="mrs align-m"/>
            <span>Authenticate with GitHub</span>
          </a>
          <span> to sync this skill with a GitHub repo</span>
        </span>
      );
    } else {
      return null;
    }
  }

  renderGithubActions() {
    if (this.props.linkedGithubRepo && this.props.isLinkedToGithub) {
      return (
        <div>
            <span className="mrm">
              <span className="type-label mrs">GitHub repository:</span>
              <GithubOwnerRepoReadonly linked={this.props.linkedGithubRepo}/>
            </span>
          {this.renderChangeRepoButton()}
        </div>
      );
    } else if (!this.props.isLinkedToGithub && !this.props.currentGroupIsModified) {
      return this.renderGithubAuth();
    } else if (!this.props.linkedGithubRepo) {
      return this.renderChangeRepoButton();
    } else {
      return null;
    }
  }

  render() {
    return (
      <div className="mtl">
        {this.renderGithubActions()}
      </div>
    );
  }
}

export default GithubRepoActions;
