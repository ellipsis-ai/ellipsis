import * as React from 'react';
import autobind from '../lib/autobind';
import LinkGithubRepo from "../behavior_editor/versions/link_github_repo";
import LinkedGithubRepo from "../models/linked_github_repo";
import {DataRequest} from "../lib/data_request";
import BehaviorGroup, {BehaviorGroupJson} from "../models/behavior_group";
import Collapsible from "../shared_ui/collapsible";

interface Props {
  teamId: string
  csrfToken: string
  onDone: () => void
}

interface State {
  linkedRepo: Option<LinkedGithubRepo>
  newGroupJson: Option<BehaviorGroupJson>
  savedGroup: Option<BehaviorGroup>
  isImporting: boolean
  isSaving: boolean
  error: Option<string>
}

interface ImportResponse {
  data: BehaviorGroupJson
}

type SaveResponse = BehaviorGroupJson

class ImportFromGithubPanel extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    autobind(this);
    this.state = {
      linkedRepo: null,
      newGroupJson: null,
      savedGroup: null,
      isImporting: false,
      isSaving: false,
      error: null
    }
  }

  onLinkGithubRepo(owner: string, repo: string, branch: Option<string>, callback: () => void): void {
    const defaultBranch = this.state.linkedRepo ? this.state.linkedRepo.currentBranch : "master";
    this.setState({
      linkedRepo: new LinkedGithubRepo(owner, repo, branch || defaultBranch),
      isImporting: true
    }, this.doImport);
  }

  doImport() {
    const repo = this.state.linkedRepo;
    if (repo) {
      DataRequest.jsonPost(jsRoutes.controllers.BehaviorEditorController.newFromGithub().url, {
        teamId: this.props.teamId,
        owner: repo.getOwner(),
        repo: repo.getRepo(),
        branch: repo.currentBranch
      }, this.props.csrfToken).then((response: ImportResponse) => {
        if (response && response.data) {
          this.setState({
            newGroupJson: response.data,
            isImporting: false,
            isSaving: true
          }, this.doSave);
        }
      })
    }
  }

  doSave() {
    const group = this.state.newGroupJson;
    if (group) {
      DataRequest.jsonPost(jsRoutes.controllers.BehaviorEditorController.save().url, {
        dataJson: group,
        isReinstall: true
      }, this.props.csrfToken).then((response: SaveResponse) => {
        this.setState({
          savedGroup: BehaviorGroup.fromJson(response),
          isSaving: false
        })
      })
    }
  }

  isReadyToLink(): boolean {
    return !this.state.savedGroup && !this.state.isImporting && !this.state.isSaving;
  }

  render() {
    const importedGroup = this.state.newGroupJson;
    const savedGroup = this.state.savedGroup;
    const hasSavedGroup = Boolean(savedGroup);
    return (
      <div className="box-action">
        <div className="container phn">
          <h4>Import from GitHub</h4>

          <Collapsible revealWhen={this.isReadyToLink()}>
            <LinkGithubRepo
              linked={this.state.linkedRepo}
              onDoneClick={this.props.onDone}
              onLinkGithubRepo={this.onLinkGithubRepo}
            />
          </Collapsible>

          <Collapsible revealWhen={this.state.isImporting}>
            <div className="pulse">
              Importing from {this.state.linkedRepo ? this.state.linkedRepo.getOwnerAndRepo() : "GitHub"}…
            </div>
          </Collapsible>

          <Collapsible revealWhen={this.state.isSaving}>
            <div className="pulse">
              <span>Saving new skill </span>
              <b>{importedGroup && importedGroup.name || "(untitled)"}…</b>
            </div>
          </Collapsible>

          <Collapsible revealWhen={hasSavedGroup}>
            {savedGroup && savedGroup.id ? (
              <div>
                <div>New skill saved: {savedGroup.getName()}</div>

                <div className="mtm">
                  <a
                    className="button button-primary"
                    href={jsRoutes.controllers.BehaviorEditorController.edit(savedGroup.id).url}
                  >Edit skill</a>
                </div>
              </div>
            ) : null}
          </Collapsible>
        </div>
      </div>
    );
  }
}

export default ImportFromGithubPanel;
