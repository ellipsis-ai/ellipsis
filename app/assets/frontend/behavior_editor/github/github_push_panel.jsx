// @flow
import * as React from 'react';
import Formatter from '../../lib/formatter';
import Button from '../../form/button';
import DynamicLabelButton from '../../form/dynamic_label_button';
import BehaviorGroup from '../../models/behavior_group';
import DataRequest from '../../lib/data_request';
import FormInput from '../../form/input';
import LinkedGithubRepo from '../../models/linked_github_repo';
import GithubErrorNotification from './github_error_notification';
import GithubOwnerRepoReadonly from './github_owner_repo_readonly';
import SVGWarning from '../../svg/warning';
import autobind from '../../lib/autobind';

type Props = {
  group: BehaviorGroup,
  linked?: LinkedGithubRepo,
  onPushBranch: () => void,
  onDoneClick: () => void,
  csrfToken: string
};

type State = {
  commitMessage: string,
  isSaving: boolean,
  lastSaved: ?Date,
  error: ?string,
  warning: ?string
};

class GithubPushPanel extends React.Component<Props, State> {
    commitMessageInput: ?FormInput;

    constructor(props: Props) {
      super(props);
      autobind(this);
      this.state = {
        commitMessage: "",
        isSaving: false,
        lastSaved: null,
        error: null,
        warning: null
      };
    }

    focus(): void {
      if (this.commitMessageInput) {
        this.commitMessageInput.focus();
      }
    }

    getBranch(): string {
      return (this.props.linked && this.props.linked.currentBranch) || "master";
    }

    getCommitMessage(): string {
      return this.state.commitMessage;
    }

    onCommitMessageChange(msg: string): void {
      this.setState({
        commitMessage: msg
      });
    }

    onPushToGithub(): void {
      if (this.props.linked) {
        const linked = this.props.linked;
        const owner = linked.getOwner();
        const repo = linked.getRepo();
        this.setState({
          isSaving: true,
          error: null,
          warning: null
        }, () => this.pushToGithub(owner, repo));
      }
    }

    pushToGithub(owner: string, repo: string): void {
      const branch = this.getBranch();
      DataRequest.jsonPost(
        jsRoutes.controllers.BehaviorEditorController.pushToGithub().url, {
          behaviorGroupId: this.props.group.id,
          owner: owner,
          repo: repo,
          branch: branch,
          commitMessage: this.getCommitMessage()
        },
        this.props.csrfToken
      ).then((json) => {
        if (json.data) {
          this.setState({
            commitMessage: "",
            isSaving: false,
            lastSaved: new Date()
          });
          this.props.onPushBranch();
        } else if (json.errors) {
          const error = json.errors;
          if (error.type && error.type === "NoChanges") {
            this.setState({
              isSaving: false,
              lastSaved: new Date(),
              warning: "Warning: nothing was committed because this branch has no changes from master."
            });
            this.props.onPushBranch();
          } else {
            this.onPushError(json.errors.message);
          }
        }
      }).catch((err: DataRequest.ResponseError) => this.onPushError(err.body || "An unknown error occurred."));
    }

    onPushError(errorMessage: string) {
      this.setState({
        isSaving: false,
        error: errorMessage
      });
    }

    onDone(): void {
      this.setState({
        commitMessage: "",
        isSaving: false,
        error: null
      }, this.props.onDoneClick);
    }

    renderContent(): React.Node {
      return (
        <div>

          <h4 className="mtn">Push current version to GitHub</h4>

          <div className="columns columns-elastic">
            <div className="column-group">
              <div className="column-row">
                <div className="column column-shrink align-button">
                  <span className="type-label mrs">Repository:</span>
                </div>
                <div className="column column-expand align-button">
                  <GithubOwnerRepoReadonly linked={this.props.linked} />
                </div>
              </div>
              <div className="column-row">
                <div className="column column-shrink align-button">
                  <span className="type-label mrs">Branch:</span>
                </div>
                <div className="column column-expand align-button">
                  <span className="type-monospace type-s width-15 mrm">{this.getBranch()}</span>
                </div>
              </div>
              <div className="column-row">
                <div className="column column-shrink align-button">
                  <span className="type-label">Commit&nbsp;message:</span>
                </div>
                <div className="column column-expand">
                  <FormInput
                    ref={(el) => this.commitMessageInput = el}
                    className="form-input-borderless type-monospace type-s mrm"
                    placeholder="Summarize what has changed"
                    onChange={this.onCommitMessageChange}
                    value={this.getCommitMessage()}
                  />
                </div>
              </div>
            </div>
          </div>

          <div className="mtxl">
            <DynamicLabelButton
              className="button-primary mrs"
              onClick={this.onPushToGithub}
              disabledWhen={this.state.isSaving || !this.getCommitMessage()}
              labels={[{
                text: "Push…",
                displayWhen: !this.state.isSaving
              }, {
                text: "Pushing…",
                displayWhen: this.state.isSaving
              }]}
            />
            <Button
              className="mrs"
              onClick={this.onDone}
            >
              {this.state.lastSaved ? "Done" : "Cancel"}
            </Button>
          </div>
          <div className="mtxl">
            {this.renderResult()}
          </div>
        </div>
      );
    }

    renderResult(): React.Node {
      if (this.state.error) {
        return (
          <GithubErrorNotification error={this.state.error} />
        );
      } else if (this.state.lastSaved && !this.state.isSaving) {
        const lastSaved = this.state.lastSaved;
        return (
          <div className="fade-in type-s">
            {this.state.warning ? (
              <span>
                <span className="display-inline-block height-xl mrs type-yellow align-m"><SVGWarning /></span>
                <b>{this.state.warning} </b>
              </span>
            ) : null}
            <span>Branch {this.getBranch()} successfully pushed {Formatter.formatTimestampRelative(lastSaved)}.</span>
          </div>
        );
      } else {
        return (
          <div>&nbsp;</div>
        );
      }
    }

    render(): React.Node {
      return (
        <div className="box-action phn">
          <div className="container container-wide">
            {this.renderContent()}
          </div>
        </div>
      );
    }
}

export default GithubPushPanel;
