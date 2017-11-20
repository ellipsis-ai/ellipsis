// @flow
define(function(require) {
  const React = require('react'),
    Button = require('../../form/button'),
    BehaviorGroup = require('../../models/behavior_group'),
    OwnerRepoReadonly = require('./github_owner_repo_readonly'),
    LinkedGithubRepo = require('../../models/linked_github_repo'),
    autobind = require('../../lib/autobind');

  type Props = {
    group: BehaviorGroup,
    linked?: LinkedGithubRepo,
    onDoneClick: () => void,
    onChangeLinkClick: () => void,
    onPullClick: () => void,
    onPushClick: () => void,
    isModified: boolean,
    csrfToken: string
  }

  class GithubChangesPanel extends React.PureComponent<Props> {
    constructor(props) {
      super(props);
      autobind(this);
    }

    renderContent(): React.Node {
      return (
        <div>
          <OwnerRepoReadonly linked={this.props.linked} onChangeLinkClick={this.props.onChangeLinkClick}/>
          <div className="mtl">
            <Button
              onClick={this.props.onPullClick}
              disabled={this.props.isModified}
            >
              Pull from GitHub…
            </Button>
            <Button
              className="mls"
              onClick={this.props.onPushClick}
              disabled={this.props.isModified}
            >
              Push to GitHub…
            </Button>
            <Button
              className="mls"
              onClick={this.props.onDoneClick}
            >
              Done
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
                <h4 className="type-weak mtn">GitHub integration</h4>
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

  return GithubChangesPanel;
});
