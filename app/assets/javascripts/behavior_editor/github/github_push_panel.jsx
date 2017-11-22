// @flow
define(function(require) {
  var React = require('react'),
    Button = require('../../form/button'),
    BehaviorGroup = require('../../models/behavior_group'),
    DataRequest = require('../../lib/data_request'),
    FormInput = require('../../form/input'),
    LinkedGithubRepo = require('../../models/linked_github_repo'),
    GithubOwnerRepoReadonly = require('./github_owner_repo_readonly'),
    SVGWarning = require('../../svg/warning'),
    autobind = require('../../lib/autobind');

  type Props = {
    group: BehaviorGroup,
    linked?: LinkedGithubRepo,
    onDoneClick: () => void,
    csrfToken: string
  };

  type State = {
    branch: string,
    commitMessage: string
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
      const owner = this.props.linked ? this.props.linked.getOwner() : "";
      const repo = this.props.linked ? this.props.linked.getRepo() : "";
      DataRequest.jsonPost(
        jsRoutes.controllers.BehaviorEditorController.pushToGithub().url, {
          behaviorGroupId: this.props.group.id,
          owner: owner,
          repo: repo,
          branch: this.getBranch(),
          commitMessage: this.getCommitMessage()
        },
        this.props.csrfToken
      ).catch(err => {
        this.setState({
          error: err.body
        });
      }).then(() => {
        if (!this.state.error) {
          this.props.onDoneClick();
        }
      });
    }

    renderErrors(): React.Node {
      if (this.state.error) {
        return (
          <div className="display-inline-block align-button mlm">
            <span className="fade-in type-pink type-bold type-italic">
              <span style={{ height: 24 }} className="display-inline-block mrs align-b"><SVGWarning /></span>
              <span>{this.state.error}</span>
            </span>
          </div>
        );
      } else {
        return null;
      }
    }

    renderContent(): React.Node {
      return (
        <div>
          <div className="columns">
            <div className="column column-one-quarter">
              <span className="display-inline-block align-m type-s type-weak mrm">Branch:</span>
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
              onChange={this.onCommitMessageChange}
              value={this.getCommitMessage()}
            />
          </div>
          <div className="mtl">
            <Button
              onClick={this.onPushToGithub}
              disabled={!this.getBranch() || !this.getCommitMessage()}
            >
              Push to Github
            </Button>
            <Button
              className="mls"
              onClick={this.props.onDoneClick}
            >
              Cancel
            </Button>
            {this.renderErrors()}
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
                <h4 className="type-weak mtn">Push code to GitHub</h4>
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
