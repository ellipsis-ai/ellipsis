// @flow
define(function(require) {
  const React = require('react'),
    Button = require('../../form/button'),
    BehaviorGroup = require('../../models/behavior_group'),
    LinkedGithubRepo = require('../../models/linked_github_repo'),
    FormInput = require('../../form/input'),
    Formatter = require('../../lib/formatter'),
    autobind = require('../../lib/autobind');

  type Props = {
    group: BehaviorGroup,
    linked?: LinkedGithubRepo,
    onDoneClick: () => void,
    onLinkGithubRepo: (string, string, () => void) => void,
    csrfToken: string
  };

  type State = {
    owner: string,
    repo: string
  };

  class GithubLinkPanel extends React.Component<Props, State> {
    props: Props;
    state: State;
    ownerInput: ?FormInput;
    repoInput: ?FormInput;

    constructor(props) {
      super(props);
      autobind(this);
      this.state = {
        owner: this.props.linked ? this.props.linked.getOwner() : "",
        repo: this.props.linked ? this.props.linked.getRepo(): ""
      };
    }

    focus(): void {
      if (this.ownerInput && !this.state.owner) {
        this.ownerInput.focus();
      } else if (this.repoInput) {
        this.repoInput.focus();
      }
    }

    isLinkModified(): boolean {
      return !this.props.linked || this.props.linked.getOwner() !== this.state.owner || this.props.linked.getRepo() !== this.state.repo;
    }

    getOwner(): string {
      return this.state.owner || "";
    }

    onOwnerChange(owner: string): void {
      this.setState({
        owner: Formatter.formatGithubUserName(owner)
      });
    }

    getRepo(): string {
      return this.state.repo || "";
    }

    onRepoChange(repo: string): void {
      this.setState({
        repo: Formatter.formatGithubRepoName(repo)
      });
    }

    onLinkClick(): void {
      this.props.onLinkGithubRepo(this.getOwner(), this.getRepo(), () => this.props.onDoneClick());
    }

    renderContent(): React.Node {
      return (
        <div>
          <div className="columns columns-elastic">
            <div className="column column-shrink align-b prxs">
              <div className="type-monospace type-s align-form-input">
                github.com://
              </div>
            </div>
            <div className="column column-shrink align-b prxs">
              <div className="type-label">Org/user name</div>
              <FormInput
                ref={(el) => this.ownerInput = el}
                className="form-input-borderless type-monospace width-15 type-s"
                placeholder="e.g. mycompany"
                onChange={this.onOwnerChange}
                value={this.getOwner()}
              />
            </div>
            <div className="column column-shrink align-b prxs">
              <div className="type-monospace type-s align-form-input">
                /
              </div>
            </div>
            <div className="column column-shrink align-b">
              <div className="type-label">Repository name</div>
              <FormInput
                ref={(el) => this.repoInput = el}
                className="form-input-borderless type-monospace width-15 type-s"
                placeholder="e.g. myrepo"
                onChange={this.onRepoChange}
                value={this.getRepo()}
              />
            </div>
          </div>
          <div className="mtl">
            <Button
              className="button-primary"
              onClick={this.onLinkClick}
              disabled={!this.getRepo() || !this.getOwner() || !this.isLinkModified() }
            >
              Link
            </Button>
            <Button
              className="mls"
              onClick={this.props.onDoneClick}
            >
              Cancel
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
                <h4 className="type-weak mtn">Link this skill to a GitHub repository</h4>
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

  return GithubLinkPanel;
});
