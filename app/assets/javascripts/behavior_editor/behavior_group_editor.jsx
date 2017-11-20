// @flow
define(function(require) {
  var React = require('react'),
    BehaviorGroup = require('../models/behavior_group'),
    Button = require('../form/button'),
    FormInput = require('../form/input'),
    LinkedGithubRepo = require('../models/linked_github_repo'),
    Textarea = require('../form/textarea'),
    autobind = require('../lib/autobind');

  type Props = {
    csrfToken: string,
    group: BehaviorGroup,
    isModified: boolean,
    isAdmin: boolean,
    isLinkedToGithub: boolean,
    linkedGithubRepo?: LinkedGithubRepo,
    onBehaviorGroupNameChange: ((string) => void),
    onBehaviorGroupDescriptionChange: ((string) => void),
    onBehaviorGroupIconChange: ((string) => void),
    onDeleteClick: (() => void),
    onSave: ((BehaviorGroup, (() => void)) => void),
    onSaveError: ((string) => void),
    onGithubIntegrationClick: (() => void)
  }

  class BehaviorGroupEditor extends React.PureComponent<Props> {
    constructor(props) {
      super(props);
      autobind(this);
      this.skillDescription = null;
      this.skillName = null;
    }

    focus(): void {
      if (this.props.group.name) {
        this.skillDescription.focus();
      } else {
        this.skillName.focus();
      }
    }

    exportGroup(): void {
      window.location = jsRoutes.controllers.BehaviorImportExportController.export(this.props.group.id).url;
    }

    getGithubAuthUrl(): string {
      const redirect = jsRoutes.controllers.BehaviorEditorController.edit(this.props.group.id).url;
      return jsRoutes.controllers.SocialAuthController.authenticateGithub(redirect).url;
    }

    renderGithubAuth(): React.Node {
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
    }

    renderGithubActions(): React.Node {
      const buttonText = this.props.linkedGithubRepo ? `Linked to ${this.props.linkedGithubRepo.getPath()}` : "Link this skill to a GitHub repo…";
      return (
        <button
          className="mls"
          type="button"
          onClick={this.props.onGithubIntegrationClick }
          disabled={ this.props.isModified }
        >
          {buttonText}
        </button>
      );
    }

    renderGithubIntegration(): React.Node {
      if (this.props.isAdmin) {
        if (this.props.isLinkedToGithub) {
          return this.renderGithubActions();
        } else {
          return this.renderGithubAuth();
        }
      } else {
        return null;
      }
    }

    render(): React.Node {
      return (
        <div className="container container-narrow mtl">

          <h5 className="type-blue-faded">Skill details</h5>

          <hr className="mvxl"/>

          <h4 className="mbn">Icon and title</h4>
          <div className="columns columns-elastic">
            <div className="column column-shrink">
              <FormInput
                className="form-input-borderless form-input-l type-l mbn width-2"
                placeholder="Icon"
                onChange={this.props.onBehaviorGroupIconChange}
                value={this.props.group.icon || ""}
              />
            </div>
            <div className="column column-expand">
              <FormInput
                ref={(el) => this.skillName = el}
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
              ref={(el) => this.skillDescription = el}
              className="form-input-height-auto"
              placeholder="Describe the general purpose of this skill (optional). The description is displayed in help."
              onChange={this.props.onBehaviorGroupDescriptionChange}
              value={this.props.group.description || ""}
              rows={"3"}
            />
          </div>

          <hr className="mvxxl"/>

          <div className="columns columns-elastic mobile-columns-float">
            <div className="column column-expand mobile-mbm">
              <Button
                onClick={this.exportGroup}
                disabled={this.props.isModified}
              >
                Export skill as ZIP file
              </Button>
              {this.renderGithubIntegration()}
            </div>

            <div className="column column-shrink">
              <Button
                className={"button-shrink"}
                onClick={this.props.onDeleteClick}
                disabled={this.props.isModified}
              >
                Delete entire skill…
              </Button>
            </div>
          </div>

        </div>
      );
    }
  }

  return BehaviorGroupEditor;
});

