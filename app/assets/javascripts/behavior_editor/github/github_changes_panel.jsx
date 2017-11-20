define(function(require) {
  var React = require('react'),
    BehaviorGroup = require('../../models/behavior_group'),
    OwnerRepoReadonly = require('./github_owner_repo_readonly'),
    LinkedGithubRepo = require('../../models/linked_github_repo');

  const GithubChangesPanel = React.createClass({
    propTypes: {
      group: React.PropTypes.instanceOf(BehaviorGroup).isRequired,
      linked: React.PropTypes.instanceOf(LinkedGithubRepo),
      onDoneClick: React.PropTypes.func.isRequired,
      onChangeLinkClick: React.PropTypes.func.isRequired,
      onPullClick: React.PropTypes.func.isRequired,
      onPushClick: React.PropTypes.func.isRequired,
      csrfToken: React.PropTypes.string.isRequired
    },

    getOwner: function() {
      return this.props.linked.getOwner();
    },

    getRepo: function() {
      return this.props.linked.getRepo();
    },

    renderContent: function() {
      return (
        <div>
          <OwnerRepoReadonly linked={this.props.linked} onChangeLinkClick={this.props.onChangeLinkClick}/>
          <div className="mtl">
            <button
              type="button"
              onClick={this.props.onPullClick}
              disabled={ this.props.isModified }
            >
              Pull from Github…
            </button>
            <button
              className="mls"
              type="button"
              onClick={this.props.onPushClick}
              disabled={ this.props.isModified }
            >
              Push to Github…
            </button>
            <button
              className="mls"
              type="button"
              onClick={this.props.onDoneClick}
            >
              Done
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
                <h4 className="type-weak mtn">Github integration</h4>
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

  return GithubChangesPanel;
});
