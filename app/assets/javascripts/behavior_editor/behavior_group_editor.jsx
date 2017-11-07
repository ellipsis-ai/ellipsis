define(function(require) {
  var React = require('react'),
    BehaviorGroup = require('../models/behavior_group'),
    DataRequest = require('../lib/data_request'),
    Input = require('../form/input'),
    Textarea = require('../form/textarea');

  return React.createClass({
    displayName: 'BehaviorGroupEditor',
    propTypes: {
      csrfToken: React.PropTypes.string.isRequired,
      group: React.PropTypes.instanceOf(BehaviorGroup).isRequired,
      isModified: React.PropTypes.bool.isRequired,
      isAdmin: React.PropTypes.bool.isRequired,
      isLinkedToGithub: React.PropTypes.bool.isRequired,
      onBehaviorGroupNameChange: React.PropTypes.func.isRequired,
      onBehaviorGroupDescriptionChange: React.PropTypes.func.isRequired,
      onBehaviorGroupIconChange: React.PropTypes.func.isRequired,
      onDeleteClick: React.PropTypes.func.isRequired,
      onSave: React.PropTypes.func.isRequired,
      onSaveError: React.PropTypes.func.isRequired
    },

    focus: function() {
      if (this.props.group.name) {
        this.refs.skillDescription.focus();
      } else {
        this.refs.skillName.focus();
      }
    },

    export: function() {
      window.location = jsRoutes.controllers.BehaviorImportExportController.export(this.props.group.id).url;
    },

    getInitialState: function() {
      return {
        githubOwner: "ellipsis-ai",
        githubRepo: "github",
        githubBranch: "master",
        commitMessage: ""
      };
    },

    getGithubOwner: function() {
      return this.state.githubOwner;
    },

    onGithubOwnerChange: function(owner) {
      this.setState({
        githubOwner: owner
      });
    },

    getGithubRepo: function() {
      return this.state.githubRepo;
    },

    onGithubRepoChange: function(repo) {
      this.setState({
        githubRepo: repo
      });
    },

    getGithubBranch: function() {
      return this.state.githubBranch;
    },

    onGithubBranchChange: function(branch) {
      this.setState({
        githubBranch: branch
      });
    },

    getGithubCommitMessage: function() {
      return this.state.commitMessage;
    },

    onGithubCommitMessageChange: function(msg) {
      this.setState({
        commitMessage: msg
      });
    },

    onUpdateFromGithub: function() {
      DataRequest.jsonPost(
        jsRoutes.controllers.BehaviorEditorController.updateFromGithub().url,
        {
          behaviorGroupId: this.props.group.id,
          owner: this.getGithubOwner(),
          repo: this.getGithubRepo(),
          branch: this.getGithubBranch()
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

    onPushToGithub: function() {
      DataRequest.jsonPost(
        jsRoutes.controllers.BehaviorEditorController.pushToGithub().url,
        {
          behaviorGroupId: this.props.group.id,
          owner: this.getGithubOwner(),
          repo: this.getGithubRepo(),
          branch: this.getGithubBranch(),
          commitMessage: this.getGithubCommitMessage()
        },
        this.props.csrfToken
      )
        .then(r => {
          console.log(r);
        });
    },

    getGithubAuthUrl: function() {
      const redirect = jsRoutes.controllers.BehaviorEditorController.edit(this.props.group.id).url;
      return jsRoutes.controllers.SocialAuthController.authenticateGithub(redirect).url;
    },

    renderGithubAuth: function() {
      return (
        <div className="columns mtxxl">
          <div className="column">
            <img height="32" src="/assets/images/logos/GitHub-Mark-64px.png"/>
          </div>
          <div className="column align-m">
            <span>To push code to or pull code from GitHub, you first need to </span>
            <a href={this.getGithubAuthUrl()}>authenticate your GitHub account</a>
          </div>
        </div>
      );
    },

    renderPullFromGithub: function() {
      return (
        <div>
          <div className="columns mtxxl">
            <div className="column column-one-quarter">
              <span className="display-inline-block align-m type-s type-weak mrm">Owner:</span>
              <Input
                ref="githubOwner"
                className="form-input-borderless type-monospace type-s width-15 mrm"
                placeholder="e.g. github"
                onChange={this.onGithubOwnerChange}
                value={this.getGithubOwner()}
              />
            </div>
            <div className="column column-one-quarter">
              <span className="display-inline-block align-m type-s type-weak mrm">Repo:</span>
              <Input
                ref="githubRepo"
                className="form-input-borderless type-monospace type-s width-15 mrm"
                placeholder="e.g. octocat"
                onChange={this.onGithubRepoChange}
                value={this.getGithubRepo()}
              />
            </div>
            <div className="column column-one-quarter">
              <span className="display-inline-block align-m type-s type-weak mrm">Branch:</span>
              <Input
                ref="githubBranch"
                className="form-input-borderless type-monospace type-s width-15 mrm"
                placeholder="e.g. master"
                onChange={this.onGithubBranchChange}
                value={this.getGithubBranch()}
              />
            </div>
          </div>
          <div className="mtl">
            <button type="button"
              onClick={this.onUpdateFromGithub}
              disabled={ this.props.isModified || !this.getGithubRepo() || !this.getGithubOwner() }
            >
              Pull latest from Github
            </button>
          </div>
        </div>
      );
    },

    renderPushToGithub: function() {
      return (
        <div>
          <div className="columns mtxxl">
            <div className="column column-one-quarter">
              <span className="display-inline-block align-m type-s type-weak mrm">Owner:</span>
              <Input
                ref="githubOwner"
                className="form-input-borderless type-monospace type-s width-15 mrm"
                placeholder="e.g. github"
                onChange={this.onGithubOwnerChange}
                value={this.getGithubOwner()}
              />
            </div>
            <div className="column column-one-quarter">
              <span className="display-inline-block align-m type-s type-weak mrm">Repo:</span>
              <Input
                ref="githubRepo"
                className="form-input-borderless type-monospace type-s width-15 mrm"
                placeholder="e.g. octocat"
                onChange={this.onGithubRepoChange}
                value={this.getGithubRepo()}
              />
            </div>
            <div className="column column-one-quarter">
              <span className="display-inline-block align-m type-s type-weak mrm">Branch:</span>
              <Input
                ref="githubBranch"
                className="form-input-borderless type-monospace type-s width-15 mrm"
                placeholder="e.g. master"
                onChange={this.onGithubBranchChange}
                value={this.getGithubBranch()}
              />
            </div>
          </div>
          <div className="mtl">
            <span className="display-inline-block align-m type-s type-weak mrm">Commit message:</span>
            <Input
              ref="commitMessage"
              className="form-input-borderless type-monospace type-s mrm"
              onChange={this.onGithubCommitMessageChange}
              value={this.getGithubCommitMessage()}
            />
          </div>
          <div className="mtl">
            <button type="button"
                    onClick={this.onPushToGithub}
                    disabled={ this.props.isModified || !this.getGithubRepo() || !this.getGithubOwner() || !this.getGithubBranch() }
            >
              Push this version to Github
            </button>
          </div>
        </div>
      );
    },

    renderGithubConfig: function() {
      return this.renderPushToGithub();
    },

    renderGithubIntegration: function() {
      if (this.props.isAdmin) {
        if (this.props.isLinkedToGithub) {
          return this.renderGithubConfig();
        } else {
          return this.renderGithubAuth();
        }
      } else {
        return null;
      }
    },

    render: function() {
      return (
        <div className="container container-narrow mtl">

          <h5 className="type-blue-faded">Skill details</h5>

          <hr className="mvxl"/>

          <h4 className="mbn">Icon and title</h4>
          <div className="columns columns-elastic">
            <div className="column column-shrink">
              <Input
                className="form-input-borderless form-input-l type-l mbn width-2"
                placeholder="Icon"
                onChange={this.props.onBehaviorGroupIconChange}
                value={this.props.group.icon || ""}
              />
            </div>
            <div className="column column-expand">
              <Input
                ref="skillName"
                className="form-input-borderless form-input-l type-l type-semibold mbn width-20"
                placeholder="Add a title (optional)"
                onChange={this.props.onBehaviorGroupNameChange}
                value={this.props.group.name || ""}
              />
            </div>
          </div>

          <h4 className="mtxxl mbs">Description</h4>
          <div>
            <Textarea
              ref="skillDescription"
              className="form-input-height-auto"
              placeholder="Describe the general purpose of this skill (optional). The description is displayed in help."
              onChange={this.props.onBehaviorGroupDescriptionChange}
              value={this.props.group.description || ""}
              rows={"3"}
            />
          </div>

          <hr className="mvxxl"/>

          <div className="columns">
            <div className="column column-one-half mobile-column-full">
              <button type="button"
                onClick={this.export}
                disabled={this.props.isModified}
              >
                Export skill as ZIP file
              </button>
            </div>

            <div className="column column-one-half align-r mobile-column-full mobile-align-l">
              <button type="button"
                onClick={this.props.onDeleteClick}
                disabled={this.props.isModified}
              >
                Delete entire skill…
              </button>
            </div>
          </div>

          {this.renderGithubIntegration()}

        </div>
      );
    }
  });
});
