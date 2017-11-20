define(function(require) {
  var React = require('react'),
    LinkedGithubRepo = require('../../models/linked_github_repo');

  const GithubOwnerRepoReadonly = React.createClass({
    propTypes: {
      linked: React.PropTypes.instanceOf(LinkedGithubRepo),
      onChangeLinkClick: React.PropTypes.func
    },

    getOwner: function() {
      return this.props.linked.getOwner();
    },

    getRepo: function() {
      return this.props.linked.getRepo();
    },

    getOwnerRepo: function() {
      return `${this.getOwner()}/${this.getRepo()}`;
    },

    renderChangeLink: function() {
      if (this.props.onChangeLinkClick) {
        return (
          <a className="pll link" onClick={this.props.onChangeLinkClick}>[Change]</a>
        );
      } else {
        return null;
      }
    },

    render: function() {
      if (this.props.linked) {
        return (
          <div>
            <div className="align-m type-s type-weak mrm">Owner/Repo:</div>
            <div className="display-inline-block align-form-input type-monospace type-s mrm">
              <span>{this.getOwnerRepo()}</span>
              {this.renderChangeLink()}
            </div>
          </div>
        );
      } else {
        return null;
      }
    },

  });

  return GithubOwnerRepoReadonly;
});
