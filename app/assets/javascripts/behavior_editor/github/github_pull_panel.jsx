define(function(require) {
  var React = require('react'),
    BehaviorGroup = require('../../models/behavior_group'),
    DataRequest = require('../../lib/data_request'),
    Input = require('../../form/input'),
    LinkedGithubRepo = require('../../models/linked_github_repo'),
    OwnerRepoReadonly = require('./github_owner_repo_readonly')
  ;

  const GithubPullPanel = React.createClass({
    propTypes: {
      group: React.PropTypes.instanceOf(BehaviorGroup).isRequired,
      linked: React.PropTypes.instanceOf(LinkedGithubRepo),
      onSave: React.PropTypes.func.isRequired,
      onSaveError: React.PropTypes.func.isRequired,
      onDoneClick: React.PropTypes.func.isRequired,
      csrfToken: React.PropTypes.string.isRequired
    },

    getInitialState: function() {
      return {
        branch: "master"
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
            <button
              type="button"
              onClick={this.onUpdateFromGithub}
              disabled={ this.props.isModified || !this.getBranch() }
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
