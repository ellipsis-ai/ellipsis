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
    GithubOwnerRepoReadonly = require('./github_owner_repo_readonly'),
    autobind = require('../../lib/autobind');

  type Props = {
    group: BehaviorGroup,
    linked?: LinkedGithubRepo,
    onDoneClick: () => void,
    csrfToken: string
  };

  type State = {
    branch: string,
    commitMessage: string,
    isSaving: boolean,
    lastSaved: ?Date,
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
        branch: "master",
        commitMessage: "",
        isSaving: false,
        lastSaved: null,
        error: null
      };
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
        branch: branch
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
      DataRequest.jsonPost(
        jsRoutes.controllers.BehaviorEditorController.pushToGithub().url, {
          behaviorGroupId: this.props.group.id,
          owner: owner,
          repo: repo,
          branch: this.getBranch(),
          commitMessage: this.getCommitMessage()
        },
        this.props.csrfToken
      ).then(() => {
        this.setState({
          commitMessage: "",
          isSaving: false,
          lastSaved: new Date()
        });
      }).catch((err: DataRequest.ResponseError) => {
        this.setState({
          isSaving: false,
          error: `An error occurred while pushing to Git (${err.status})`
        });
      });
    }

    onDone(): void {
      this.setState({
        commitMessage: "",
        isSaving: false
      }, this.props.onDoneClick);
    }

    renderContent(): React.Node {
      return (
        <div>

          <h4>Push to GitHub</h4>
          <p>To push the current version of the skill to GitHub, verify the target branch name and write a commit message.</p>

          <div className="columns">
            <div className="column column-one-quarter">
              <span className="display-inline-block align-m type-s type-weak mrm">Branch name:</span>
              <FormInput
                ref={(el) => this.branchInput = el}
                className="form-input-borderless type-monospace type-s width-15 mrm"
                placeholder="e.g. master"
                onChange={this.onBranchChange}
                value={this.getBranch()}
              />
            </div>
          </div>
          <div className="mtl">
            <span className="display-inline-block align-m type-s type-weak mrm">Commit message:</span>
            <FormInput
              ref={(el) => this.commitMessageInput = el}
              className="form-input-borderless type-monospace type-s mrm"
              placeholder="Summarize what has changed"
              onChange={this.onCommitMessageChange}
              value={this.getCommitMessage()}
            />
          </div>
          <div className="mtl">
            <DynamicLabelButton
              className="button-primary mrs mbs"
              onClick={this.onPushToGithub}
              disabledWhen={this.state.isSaving || !this.getBranch() || !this.getCommitMessage()}
              labels={[{
                text: "Commit and push…",
                displayWhen: !this.state.isSaving
              }, {
                text: "Pushing…",
                displayWhen: this.state.isSaving
              }]}
            />
            <Button
              className="mrs mbs"
              onClick={this.onDone}
            >
              Done
            </Button>
            {!this.state.error && this.state.lastSaved && !this.state.isSaving ? (
              <span className="align-button mbs type-green">
                — Successfully pushed {Formatter.formatTimestampRelative(this.state.lastSaved)}
              </span>
            ) : null}
            {this.state.error ? (
              <span className="align-button mbs type-pink type-bold type-italic">— {this.state.error}</span>
            ) : null}
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
                <h4 className="type-weak mtn">GitHub</h4>
                <GithubOwnerRepoReadonly linked={this.props.linked}/>
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

  return GithubPushPanel;
});
