// @flow
define(function(require) {
  var React = require('react'),
    Button = require('../../form/button'),
    BehaviorGroup = require('../../models/behavior_group'),
    DataRequest = require('../../lib/data_request'),
    FormInput = require('../../form/input'),
    LinkedGithubRepo = require('../../models/linked_github_repo'),
    OwnerRepoReadonly = require('./github_owner_repo_readonly'),
    autobind = require('../../lib/autobind');

  type Props = {
    group: BehaviorGroup,
    linked?: LinkedGithubRepo,
    onDoneClick: () => void,
    csrfToken: string
  }

  class GithubPullPanel extends React.Component<Props> {
    constructor(props) {
      super(props);
      autobind(this);
      this.state = {
        branch: "master",
        commitMessage: ""
      };
    }

    getOwner(): string {
      return this.props.linked.getOwner();
    }

    getRepo(): string {
      return this.props.linked.getRepo();
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
      DataRequest.jsonPost(
        jsRoutes.controllers.BehaviorEditorController.pushToGithub().url, {
          behaviorGroupId: this.props.group.id,
          owner: this.getOwner(),
          repo: this.getRepo(),
          branch: this.getBranch(),
          commitMessage: this.getCommitMessage()
        },
        this.props.csrfToken
      ).then(() => {
        this.props.onDoneClick();
      });
    }

    renderContent(): React.Node {
      return (
        <div>
          <div className="columns">
            <div className="column column-one-quarter">
              <OwnerRepoReadonly linked={this.props.linked}/>
            </div>
            <div className="column column-one-quarter">
              <span className="display-inline-block align-m type-s type-weak mrm">Branch:</span>
              <FormInput
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

  return GithubPullPanel;
});
