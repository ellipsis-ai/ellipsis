define(function(require) {
  var React = require('react'),
    BehaviorGroup = require('../models/behavior_group'),
    DataRequest = require('../lib/data_request'),
    Input = require('../form/input')
  ;

  const GithubPullPanel = React.createClass({
    propTypes: {
      group: React.PropTypes.instanceOf(BehaviorGroup).isRequired,
      onSave: React.PropTypes.func.isRequired,
      onSaveError: React.PropTypes.func.isRequired,
      onDoneClick: React.PropTypes.func.isRequired,
      csrfToken: React.PropTypes.string.isRequired
    },

    getInitialState: function() {
      return {
        owner: "ellipsis-ai",
        repo: "github",
        branch: "master"
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

    onUpdateFromGithub: function() {
      DataRequest.jsonPost(
        jsRoutes.controllers.BehaviorEditorController.updateFromGithub().url,
        {
          behaviorGroupId: this.props.group.id,
          owner: this.getOwner(),
          repo: this.getRepo(),
          branch: this.getBranch()
        },
        this.props.csrfToken
      )
        .then((json) => {
          if (json.errors) {
            this.props.onSaveError(json.errors);
          } else {
            this.props.onSave(BehaviorGroup.fromJson(json.data));
          }
        })
        .catch((error) => {
          this.props.onSaveError(error);
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
            <button
              type="button"
              onClick={this.onUpdateFromGithub}
              disabled={ this.props.isModified || !this.getRepo() || !this.getOwner() || !this.getBranch() }
            >
              Pull from Github
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
                <h4 className="type-weak mtn">Pull code from GitHub</h4>
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