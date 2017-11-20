define(function(require) {
  var React = require('react'),
    BehaviorGroup = require('../../models/behavior_group'),
    LinkedGithubRepo = require('../../models/linked_github_repo'),
    Input = require('../../form/input');

  const GithubLinkPanel = React.createClass({
    propTypes: {
      group: React.PropTypes.instanceOf(BehaviorGroup).isRequired,
      linked: React.PropTypes.instanceOf(LinkedGithubRepo),
      onDoneClick: React.PropTypes.func.isRequired,
      onLinkGithubRepo: React.PropTypes.func.isRequired,
      csrfToken: React.PropTypes.string.isRequired
    },

    getInitialState: function() {
      return {
        owner: this.props.linked ? this.props.linked.getOwner() : "",
        repo: this.props.linked ? this.props.linked.getRepo(): ""
      };
    },

    isLinkModified: function() {
      return !this.props.linked || this.props.linked.getOwner() !== this.state.owner || this.props.linked.getRepo() !== this.state.repo;
    },

    getOwner: function() {
      return this.state.owner || "";
    },

    onOwnerChange: function(owner) {
      this.setState({
        owner: owner
      });
    },

    getRepo: function() {
      return this.state.repo || "";
    },

    onRepoChange: function(repo) {
      this.setState({
        repo: repo
      });
    },

    onLinkClick: function() {
      this.props.onLinkGithubRepo(this.getOwner(), this.getRepo(), () => this.props.onDoneClick());
    },

    renderContent: function() {
      return (
        <div>
          <div className="columns">
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
          </div>
          <div className="mtl">
            <button
              type="button"
              onClick={this.onLinkClick}
              disabled={ this.props.isModified || !this.getRepo() || !this.getOwner() || !this.isLinkModified() }
            >
              Link
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
                <h4 className="type-weak mtn">Link this skill to a GitHub repo</h4>
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

  return GithubLinkPanel;
});
