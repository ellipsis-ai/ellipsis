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
    onSave: (BehaviorGroup, callback?: () => void) => void,
    onDoneClick: () => void,
    csrfToken: string
  }

  type State = {
    branch: string,
    isFetching: boolean,
    lastFetched: ?Date,
    lastFetchedBranch: ?string,
    error: ?string
  }

  class GithubPullPanel extends React.Component<Props, State> {
    props: Props;
    state: State;
    branchInput: ?FormInput;

    constructor(props) {
      super(props);
      autobind(this);
      this.state = {
        branch: "master",
        isFetching: false,
        lastFetched: null,
        lastFetchedBranch: null,
        error: null
      };
    }

    focus(): void {
      if (this.branchInput) {
        this.branchInput.focus();
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

    onUpdateFromGithub(): void {
      if (this.props.linked) {
        const linked = this.props.linked;
        const owner = linked.getOwner();
        const repo = linked.getRepo();
        const branch = this.getBranch();
        this.setState({
          isFetching: true,
          error: null
        }, () => this.updateFromGitHub(owner, repo, branch));
      }
    }

    updateFromGitHub(owner: string, repo: string, branch: string): void {
      DataRequest.jsonPost(
        jsRoutes.controllers.BehaviorEditorController.updateFromGithub().url, {
          behaviorGroupId: this.props.group.id,
          owner: owner,
          repo: repo,
          branch: branch
        },
        this.props.csrfToken
      ).then((json) => {
        if (json.errors) {
          this.onError(branch, json.errors);
        } else {
          this.setState({
            isFetching: false,
            lastFetched: new Date(),
            lastFetchedBranch: branch
          }, () => this.props.onSave(BehaviorGroup.fromJson(json.data)));
        }
      }).catch(() => {
        this.onError(branch);
      });
    }

    onError(branch: string, error?: string): void {
      this.setState({
        isFetching: false,
        error: `An error occurred while pulling “${branch}” from GitHub ${error ? `(${error})` : ""}`
      });
    }

    renderContent(): React.Node {
      return (
        <div>

          <h4>Pull from GitHub</h4>
          <p>Confirm the branch name, then pull to save that branch as the current skill version.</p>

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
            <DynamicLabelButton
              className="mrs mbs"
              onClick={this.onUpdateFromGithub}
              disabledWhen={this.state.isFetching || !this.getBranch()}
              labels={[{
                text: "Pull and save…",
                displayWhen: !this.state.isFetching
              }, {
                text: "Pulling…",
                displayWhen: this.state.isFetching
              }]}
            />
            <Button
              className="mrs mbs"
              onClick={this.props.onDoneClick}
            >
              Done
            </Button>
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
      } else if (this.state.lastFetched) {
        const branch = this.state.lastFetchedBranch ? `from branch ${this.state.lastFetchedBranch}` : "";
        const text = `— Last pulled ${branch} ${Formatter.formatTimestampRelative(this.state.lastFetched)}`;
        return (
          <span className="align-button mbs type-green">{text}</span>
        );
      }
    }

    render(): React.Node {
      return (
        <div className="box-action phn">
          <div className="container">
            <div className="columns">
              <div className="column column-page-sidebar">
                <h4 className="type-weak mtn">Link with GitHub</h4>
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
