define(function(require) {
  var React = require('react'),
    BehaviorGroup = require('../models/behavior_group'),
    DataRequest = require('../lib/data_request'),
    Input = require('../form/input'),
    LinkedGithubRepo = require('../models/linked_github_repo'),
    OwnerRepoReadonly = require('./github_owner_repo_readonly')
  ;

  const GithubPullPanel = React.createClass({
    propTypes: {
      group: React.PropTypes.instanceOf(BehaviorGroup).isRequired,
      linked: React.PropTypes.instanceOf(LinkedGithubRepo),
      onDoneClick: React.PropTypes.func.isRequired,
      csrfToken: React.PropTypes.string.isRequired
    },

    getInitialState: function() {
      return {
        branch: "master",
        commitMessage: ""
      };
    },

    getOwner: function() {
      return this.props.linked.getOwner();
    },

    getRepo: function() {
      return this.props.linked.getRepo();
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
          <div className="columns">
            <div className="column column-one-quarter">
              <OwnerRepoReadonly linked={this.props.linked}/>
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
