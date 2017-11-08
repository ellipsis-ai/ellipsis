define(function(require) {
  var React = require('react'),
    BehaviorGroup = require('../models/behavior_group'),
    DataRequest = require('../lib/data_request'),
    Input = require('../form/input')
  ;

  const GithubPullPanel = React.createClass({
    propTypes: {
      group: React.PropTypes.instanceOf(BehaviorGroup).isRequired,
      onDoneClick: React.PropTypes.func.isRequired,
      csrfToken: React.PropTypes.string.isRequired
    },

    getInitialState: function() {
      return {
        owner: "ellipsis-ai",
        repo: "github",
        branch: "master",
        commitMessage: ""
      };
    },

    getOwner: function() {
      return this.state.owner;
    },

    onOwnerChange: function(owner) {
      this.setState({
        owner: owner
      });
    },

    getRepo: function() {
      return this.state.repo;
    },

    onRepoChange: function(repo) {
      this.setState({
        repo: repo
      });
    },

    getBranch: function() {
      return this.state.branch;
    },

    onBranchChange: function(branch) {
      this.setState({
        branch: branch
      });
    },

    getCommitMessage: function() {
      return this.state.commitMessage;
    },

    onCommitMessageChange: function(msg) {
      this.setState({
        commitMessage: msg
      });
    },

    onPushToGithub: function() {
      DataRequest.jsonPost(
        jsRoutes.controllers.BehaviorEditorController.pushToGithub().url,
        {
          behaviorGroupId: this.props.group.id,
          owner: this.getOwner(),
          repo: this.getRepo(),
          branch: this.getBranch(),
          commitMessage: this.getCommitMessage()
        },
        this.props.csrfToken
      )
        .then(r => {
          this.props.onDoneClick();
        });
    },

    renderContent: function() {
      return (
        <div>
          <div className="columns mtxxl">
            <div className="column column-one-quarter">
              <span className="display-inline-block align-m type-s type-weak mrm">Owner:</span>
              <Input
                ref="githubOwner"
                className="form-input-borderless type-monospace type-s width-15 mrm"
                placeholder="e.g. github"
                onChange={this.onOwnerChange}
                value={this.getOwner()}
              />
            </div>
            <div className="column column-one-quarter">
              <span className="display-inline-block align-m type-s type-weak mrm">Repo:</span>
              <Input
                ref="githubRepo"
                className="form-input-borderless type-monospace type-s width-15 mrm"
                placeholder="e.g. octocat"
                onChange={this.onRepoChange}
                value={this.getRepo()}
              />
            </div>
            <div className="column column-one-quarter">
              <span className="display-inline-block align-m type-s type-weak mrm">Branch:</span>
              <Input
                ref="githubBranch"
                className="form-input-borderless type-monospace type-s width-15 mrm"
                placeholder="e.g. master"
                onChange={this.onBranchChange}
                value={this.getBranch()}
              />
            </div>
          </div>
          <div className="mtl">
            <span className="display-inline-block align-m type-s type-weak mrm">Commit message:</span>
            <Input
              ref="commitMessage"
              className="form-input-borderless type-monospace type-s mrm"
              onChange={this.onCommitMessageChange}
              value={this.getCommitMessage()}
            />
          </div>
          <div className="mtl">
            <button
              type="button"
              onClick={this.onPushToGithub}
              disabled={ this.props.isModified || !this.getRepo() || !this.getOwner() || !this.getBranch() || !this.getCommitMessage() }
            >
              Push to Github
            </button>
            <button
              className="mls"
              type="button"
              onClick={this.props.onDoneClick}
            >
              Cancel
            </button>
          </div>
        </div>
      );
    },

    render: function() {
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
    },

  });

  return GithubPullPanel;
});
