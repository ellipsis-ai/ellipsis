// @flow
define(function(require) {
  var React = require('react'),
    Button = require('../../form/button'),
    BehaviorGroup = require('../../models/behavior_group'),
    DataRequest = require('../../lib/data_request'),
    FormInput = require('../../form/input'),
    LinkedGithubRepo = require('../../models/linked_github_repo'),
    GithubOwnerRepoReadonly = require('./github_owner_repo_readonly'),
    autobind = require('../../lib/autobind');

  type Props = {
    group: BehaviorGroup,
    linked?: LinkedGithubRepo,
    onSave: (BehaviorGroup, callback?: () => void) => void,
    onSaveError: (string) => void,
    onDoneClick: () => void,
    csrfToken: string
  }

  type State = {
    branch: string
  }

  class GithubPullPanel extends React.Component<Props, State> {
    props: Props;
    state: State;
    branchInput: ?FormInput;

    constructor(props) {
      super(props);
      autobind(this);
      this.state = {
        branch: "master"
      };
    }

    focus(): void {
      if (this.branchInput) {
        this.branchInput.focus();
      }
    }

    getOwner(): string {
      return this.props.linked ? this.props.linked.getOwner() : "";
    }

    getRepo(): string {
      return this.props.linked ? this.props.linked.getRepo() : "";
    }

    getBranch(): string {
      return this.state.branch;
    }

    onBranchChange(branch: string): void {
      this.setState({
        branch: branch
      });
    }

    onUpdateFromGithub(): void {
      DataRequest.jsonPost(
        jsRoutes.controllers.BehaviorEditorController.updateFromGithub().url, {
          behaviorGroupId: this.props.group.id,
          owner: this.getOwner(),
          repo: this.getRepo(),
          branch: this.getBranch()
        },
        this.props.csrfToken
      ).then((json) => {
        if (json.errors) {
          this.props.onSaveError(json.errors);
        } else {
          this.props.onSave(BehaviorGroup.fromJson(json.data));
        }
      }).catch((error) => {
        this.props.onSaveError(error);
      });
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
            <Button
              onClick={this.onUpdateFromGithub}
              disabled={!this.getBranch()}
            >
              Pull from Github
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
                <h4 className="type-weak mtn">GitHub</h4>
                <GithubOwnerRepoReadonly linked={this.props.linked} />
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
