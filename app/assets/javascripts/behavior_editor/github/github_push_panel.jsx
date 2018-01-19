// @flow
define(function(require) {
  var React = require('react'),
    Formatter = require('../../lib/formatter'),
    Button = require('../../form/button'),
    DynamicLabelButton = require('../../form/dynamic_label_button'),
    BehaviorGroup = require('../../models/behavior_group'),
    DataRequest = require('../../lib/data_request'),
    FormInput = require('../../form/input'),
    LinkedGithubRepo = require('../../models/linked_github_repo'),
    GithubErrorNotification = require('./github_error_notification'),
    GithubOwnerRepoReadonly = require('./github_owner_repo_readonly'),
    autobind = require('../../lib/autobind');

  type Props = {
    group: BehaviorGroup,
    linked?: LinkedGithubRepo,
    onPushBranch: () => void,
    onDoneClick: () => void,
    csrfToken: string,
    branch: ?string
  };

  type State = {
    branch: string,
    commitMessage: string,
    isSaving: boolean,
    lastSaved: ?Date,
    lastSavedBranch: ?string,
    error: ?string
  };

  class GithubPushPanel extends React.Component<Props, State> {
    props: Props;
    state: State;
    branchInput: ?FormInput;
    commitMessageInput: ?FormInput;

    constructor(props) {
      super(props);
      autobind(this);
      this.state = {
        branch: this.getDefaultBranch(),
        commitMessage: "",
        isSaving: false,
        lastSaved: null,
        lastSavedBranch: null,
        error: null
      };
    }

    componentWillReceiveProps(newProps) {
      if (newProps.branch !== this.props.branch) {
        this.setState({
          branch: newProps.branch
        });
      }
    }

    getDefaultBranch(): string {
      return this.props.branch || "master";
    }

    focus(): void {
      if (this.branchInput && !this.getBranch()) {
        this.branchInput.focus();
      } else if (this.commitMessageInput) {
        this.commitMessageInput.focus();
      }
    }

    getBranch(): string {
      return this.state.branch;
    }

    onBranchChange(branch: string): void {
      this.setState({
        branch: Formatter.formatGitBranchIdentifier(branch)
      });
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
          error: null
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
      ).then(() => {
        this.setState({
          commitMessage: "",
          isSaving: false,
          lastSaved: new Date(),
          lastSavedBranch: branch
        });
        this.props.onPushBranch();
      }).catch((err: DataRequest.ResponseError) => {
        this.setState({
          isSaving: false,
          error: err.body
        });
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
                <div className="column column-expand">
                  <FormInput
                    ref={(el) => this.branchInput = el}
                    className="form-input-borderless type-monospace type-s width-15 mrm"
                    placeholder="e.g. master"
                    onChange={this.onBranchChange}
                    value={this.getBranch()}
                  />
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
              disabledWhen={this.state.isSaving || !this.getBranch() || !this.getCommitMessage()}
              labels={[{
                text: "Force push…",
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
              Done
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
        const branch = this.state.lastSavedBranch ? `to branch ${this.state.lastSavedBranch}` : "";
        return (
          <div className="fade-in">
            Pushed {branch} {Formatter.formatTimestampRelative(this.state.lastSaved)}
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

  return GithubPushPanel;
});
