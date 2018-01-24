requirejs(['common'], function() {
  requirejs(
    ['core-js', 'whatwg-fetch', 'react', 'react-dom', './lib/browser_utils', './behavior_editor/index',
      './models/behavior_group', './models/behavior_group_deployment', 'config/behavioreditor/edit', './models/param_type', './models/aws',
      './models/oauth2', './models/simple_token', './models/linked_github_repo', './lib/autobind', './shared_ui/page', './lib/data_request'],
    function(Core, Fetch, React, ReactDOM, BrowserUtils, BehaviorEditor,
             BehaviorGroup, BehaviorGroupDeployment, BehaviorEditorConfiguration, ParamType, aws,
             oauth2, simpleToken, LinkedGithubRepo, autobind, Page, DataRequest) {

      class BehaviorEditorLoader extends React.Component {
        constructor(props) {
          super(props);
          autobind(this);
          const group = BehaviorGroup.fromJson(this.props.group);
          const selectedId = this.props.selectedId || this.fallbackSelectedIdFor(group);
          this.state = {
            awsConfigs: this.props.awsConfigs.map(aws.AWSConfigRef.fromJson),
            oauth2Applications: this.props.oauth2Applications.map(oauth2.OAuth2ApplicationRef.fromJson),
            simpleTokenApis: this.props.simpleTokenApis.map(simpleToken.SimpleTokenApiRef.fromJson),
            linkedGithubRepo: this.props.linkedGithubRepo ? LinkedGithubRepo.fromJson(this.props.linkedGithubRepo) : undefined,
            group: group,
            builtinParamTypes: this.props.builtinParamTypes.map(ParamType.fromJson),
            selectedId: selectedId,
            onLoad: null
          };
          if (group.id && selectedId) {
            BrowserUtils.replaceURL(jsRoutes.controllers.BehaviorEditorController.edit(group.id, selectedId, this.props.showVersions || null).url);
          }
        }

        onLinkGithubRepo(owner, repo, callback) {
          DataRequest.jsonPost(
            jsRoutes.controllers.BehaviorEditorController.linkToGithubRepo().url,
            {
              behaviorGroupId: this.props.group.id,
              owner: owner,
              repo: repo
            },
            this.props.csrfToken
          )
            .then(() => {
              const linked = new LinkedGithubRepo({ owner: owner, repo: repo });
              this.setState({ linkedGithubRepo: linked }, callback);
            });
        }

        onSave(newProps) {
          const newState = {
            group: newProps.group,
            onLoad: newProps.onLoad
          };
          this.setState(newState);
        }

        onDeploy(deploymentProps, callback) {
          this.setState({
            group: this.state.group.clone({ deployment: BehaviorGroupDeployment.fromProps(deploymentProps) }),
            onLoad: null
          }, callback);
        }

        fallbackSelectedIdFor(group) {
          var isSimpleBehaviorGroup = !group.name && !group.description && group.behaviorVersions.length === 1;
          if (isSimpleBehaviorGroup) {
            return group.behaviorVersions[0].behaviorId;
          } else {
            return null;
          }
        }

        resetSavedAnswerForInput(inputId, numAnswersDeleted) {
          const newSavedAnswers = this.state.savedAnswers.map((ea) => {
            if (ea.inputId === inputId) {
              return Object.assign({}, ea, {
                myValueString: null,
                userAnswerCount: ea.userAnswerCount - numAnswersDeleted
              });
            } else {
              return ea;
            }
          });
          this.setState({
            savedAnswers: newSavedAnswers
          });
        }

        render() {
          return (
            <Page csrfToken={this.props.csrfToken}>
              <BehaviorEditor
                group={this.state.group}
                selectedId={this.state.selectedId}
                csrfToken={this.props.csrfToken}
                builtinParamTypes={this.state.builtinParamTypes}
                envVariables={this.props.envVariables}
                awsConfigs={this.state.awsConfigs}
                oauth2Applications={this.state.oauth2Applications}
                oauth2Apis={this.props.oauth2Apis}
                simpleTokenApis={this.state.simpleTokenApis}
                linkedOAuth2ApplicationIds={this.props.linkedOAuth2ApplicationIds}
                savedAnswers={this.props.savedAnswers}
                onSave={this.onSave}
                onForgetSavedAnswerForInput={this.resetSavedAnswerForInput}
                onLoad={this.state.onLoad}
                userId={this.props.userId}
                isAdmin={this.props.isAdmin}
                isLinkedToGithub={this.props.isLinkedToGithub}
                linkedGithubRepo={this.state.linkedGithubRepo}
                onLinkGithubRepo={this.onLinkGithubRepo}
                showVersions={this.props.showVersions}
                onDeploy={this.onDeploy}
                lastDeployTimestamp={this.props.lastDeployTimestamp}
              />
            </Page>
          );
        }
      }

      BehaviorEditorLoader.propTypes = {
        containerId: React.PropTypes.string.isRequired,
        csrfToken: React.PropTypes.string.isRequired,
        group: React.PropTypes.object.isRequired,
        selectedId: React.PropTypes.string,
        builtinParamTypes: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
        envVariables: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
        savedAnswers: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
        awsConfigs: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
        oauth2Applications: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
        oauth2Apis: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
        simpleTokenApis: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
        linkedOAuth2ApplicationIds: React.PropTypes.arrayOf(React.PropTypes.string).isRequired,
        userId: React.PropTypes.string.isRequired,
        isAdmin: React.PropTypes.bool.isRequired,
        isLinkedToGithub: React.PropTypes.bool.isRequired,
        linkedGithubRepo: React.PropTypes.object,
        showVersions: React.PropTypes.bool,
        lastDeployTimestamp: React.PropTypes.string
      };

      ReactDOM.render(
        React.createElement(BehaviorEditorLoader, BehaviorEditorConfiguration),
        document.getElementById(BehaviorEditorConfiguration.containerId)
      );

    }
  );
});
