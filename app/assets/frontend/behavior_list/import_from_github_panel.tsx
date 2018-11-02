import * as React from 'react';
import autobind from '../lib/autobind';
import LinkGithubRepo from "../behavior_editor/versions/link_github_repo";
import LinkedGithubRepo from "../models/linked_github_repo";
import {DataRequest, ResponseError} from "../lib/data_request";
import BehaviorGroup, {BehaviorGroupJson} from "../models/behavior_group";
import Collapsible from "../shared_ui/collapsible";
import {DynamicLabelButtonLabel} from "../form/dynamic_label_button";
import {GithubFetchError} from "../models/github/github_fetch_error";

interface Props {
  isActive: boolean
  teamId: string
  csrfToken: string
  onDone: () => void
  onBehaviorGroupImport: (newGroup: BehaviorGroup) => void
  onIsImportingToTeam: (group: BehaviorGroup) => boolean
}

interface State {
  linkedRepo: Option<LinkedGithubRepo>
  newGroup: Option<BehaviorGroup>
  savedGroup: Option<BehaviorGroup>
  isImportingFromGithub: boolean
  error: Option<string>
}

interface ImportResponse {
  data?: BehaviorGroupJson
  errors?: GithubFetchError
}

class ImportFromGithubPanel extends React.Component<Props, State> {
  githubRepoInput: Option<LinkGithubRepo>;

  constructor(props: Props) {
    super(props);
    autobind(this);
    this.state = this.getDefaultState();
  }

  getDefaultState(): State {
    return {
      linkedRepo: null,
      newGroup: null,
      savedGroup: null,
      isImportingFromGithub: false,
      error: null
    };
  }

  componentWillReceiveProps(newProps: Props) {
    if (this.props.isActive && !newProps.isActive) {
      this.setState(this.getDefaultState);
    }
  }

  componentDidUpdate(prevProps: Props) {
    if (!prevProps.isActive && this.props.isActive && this.githubRepoInput) {
      this.githubRepoInput.focus();
    }
  }

  onLinkGithubRepo(owner: string, repo: string, branch: Option<string>, callback: () => void): void {
    const defaultBranch = this.state.linkedRepo ? this.state.linkedRepo.currentBranch : "master";
    this.setState({
      error: null,
      newGroup: null,
      linkedRepo: new LinkedGithubRepo(owner, repo, branch || defaultBranch),
      isImportingFromGithub: true
    }, this.doImportFromGithub);
  }

  doImportFromGithub() {
    const repo = this.state.linkedRepo;
    if (repo) {
      DataRequest.jsonPost(jsRoutes.controllers.BehaviorEditorController.newFromGithub().url, {
        teamId: this.props.teamId,
        owner: repo.getOwner(),
        repo: repo.getRepo(),
        branch: repo.currentBranch
      }, this.props.csrfToken).then((response: ImportResponse) => {
        if (response.data) {
          const group = BehaviorGroup.fromJson(response.data);
          this.setState({
            newGroup: group,
            isImportingFromGithub: false
          }, () => {
            this.props.onBehaviorGroupImport(group);
          });
        } else if (response.errors) {
          this.setState({
            linkedRepo: null,
            isImportingFromGithub: false,
            error: response.errors.message
          });
        } else {
          throw new ResponseError(200, "Unexpected response received", null)
        }
      }).catch((err: ResponseError) => {
        this.setState({
          linkedRepo: null,
          isImportingFromGithub: false,
          error: err.body || err.statusText
        });
      });
    }
  }

  isBusy(): boolean {
    return Boolean(this.isImporting() || this.state.newGroup);
  }

  isImporting(): boolean {
    return this.state.isImportingFromGithub;
  }

  isInstalling(): boolean {
    return Boolean(this.state.newGroup && this.props.onIsImportingToTeam(this.state.newGroup));
  }

  getLinkButtonLabels(): Array<DynamicLabelButtonLabel> {
    return [{
      text: "Import",
      displayWhen: !this.isBusy()
    }, {
      text: "Importing…",
      displayWhen: this.isBusy()
    }]
  }

  render() {
    const source = this.state.linkedRepo ? this.state.linkedRepo.getOwnerAndRepo() : "GitHub";
    return (
      <div className="box-action">
        <div className="container container-wide">
          <div className="columns">
            <div className="column column-page-sidebar">
              <h4 className="mtn">Import from GitHub</h4>

              <div className="type-s">
                <Collapsible revealWhen={this.isBusy()}>
                  <div className={this.isImporting() ? "pulse" : "type-green"}>
                    {this.isImporting() ? `Importing from ${source}` : `Imported skill from ${source}`}
                  </div>
                  {this.state.newGroup && this.isInstalling() ? (
                    <div className="pulse">
                      <span>Installing </span>
                      {this.state.newGroup.icon ? (
                        <span>{this.state.newGroup.icon} </span>
                      ) : null}
                      <b>{this.state.newGroup.getName()}</b>
                    </div>
                  ) : null}
                </Collapsible>
              </div>
            </div>

            <div className="column column-page-main">
              <LinkGithubRepo
                ref={(el) => this.githubRepoInput = el}
                linked={this.state.linkedRepo}
                onDoneClick={this.props.onDone}
                onLinkGithubRepo={this.onLinkGithubRepo}
                isLinking={this.isBusy()}
                linkButtonLabels={this.getLinkButtonLabels()}
                error={this.state.error}
              />
            </div>
          </div>
        </div>
      </div>
    );
  }
}

export default ImportFromGithubPanel;
