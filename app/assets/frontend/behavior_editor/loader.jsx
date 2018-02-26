/* global BehaviorEditorConfiguration:false */
import 'core-js';
import 'whatwg-fetch';
import * as React from 'react';
import * as ReactDOM from 'react-dom';
import BrowserUtils from '../lib/browser_utils';
import BehaviorEditor from './index';
import BehaviorGroup from '../models/behavior_group';
import BehaviorGroupDeployment from '../models/behavior_group_deployment';
import ParamType from '../models/param_type';
import {AWSConfigRef} from '../models/aws';
import {OAuth2ApplicationRef} from '../models/oauth2';
import {SimpleTokenApiRef} from '../models/simple_token';
import LinkedGithubRepo from '../models/linked_github_repo';
import autobind from '../lib/autobind';
import Page from '../shared_ui/page';
import DataRequest from '../lib/data_request';

class BehaviorEditorLoader extends React.Component {
        constructor(props) {
          super(props);
          autobind(this);
          const group = BehaviorGroup.fromJson(this.props.group);
          const selectedId = this.props.selectedId || this.fallbackSelectedIdFor(group);
          this.state = {
            awsConfigs: this.props.awsConfigs.map(AWSConfigRef.fromJson),
            oauth2Applications: this.props.oauth2Applications.map(OAuth2ApplicationRef.fromJson),
            simpleTokenApis: this.props.simpleTokenApis.map(SimpleTokenApiRef.fromJson),
            linkedGithubRepo: this.props.linkedGithubRepo ? LinkedGithubRepo.fromJson(this.props.linkedGithubRepo) : undefined,
            group: group,
            builtinParamTypes: this.props.builtinParamTypes.map(ParamType.fromJson),
            selectedId: selectedId,
            savedAnswers: this.props.savedAnswers,
            onLoad: null
          };
          if (group.id && selectedId) {
            BrowserUtils.replaceURL(jsRoutes.controllers.BehaviorEditorController.edit(group.id, selectedId, this.props.showVersions || null).url);
          }
        }

        onLinkGithubRepo(owner, repo, branch, callback) {
          const url = jsRoutes.controllers.BehaviorEditorController.linkToGithubRepo().url;
          const params = {};
          params.behaviorGroupId = this.props.group.id;
          params.owner = owner;
          params.repo = repo;
          if (branch) {
            params.currentBranch = branch;
          }
          DataRequest.jsonPost(url, params, this.props.csrfToken)
            .then(() => {
              const linked = new LinkedGithubRepo(owner, repo, branch);
              this.setState({ linkedGithubRepo: linked }, callback);
            });
        }

        onUpdateFromGithub(owner, repo, branch, callback, onError) {
          DataRequest.jsonPost(
            jsRoutes.controllers.BehaviorEditorController.updateFromGithub().url, {
              behaviorGroupId: this.props.group.id,
              owner: owner,
              repo: repo,
              branch: branch
            },
            this.props.csrfToken
          ).then((json) => {
            this.setState({
              linkedGithubRepo: new LinkedGithubRepo(owner, repo, branch)
            }, () => {
              if (json.errors) {
                onError(branch, json.errors);
              } else {
                callback(json);
              }
            });
          }).catch(() => {
            onError(branch, null);
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

        getSavedAnswers() {
          return (this.state && this.state.savedAnswers) ? this.state.savedAnswers : [];
        }

        resetSavedAnswerForInput(inputId, numAnswersDeleted) {
          const newSavedAnswers = this.getSavedAnswers().map((ea) => {
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
                savedAnswers={this.getSavedAnswers()}
                onSave={this.onSave}
                onForgetSavedAnswerForInput={this.resetSavedAnswerForInput}
                onLoad={this.state.onLoad}
                userId={this.props.userId}
                isAdmin={this.props.isAdmin}
                isLinkedToGithub={this.props.isLinkedToGithub}
                linkedGithubRepo={this.state.linkedGithubRepo}
                onLinkGithubRepo={this.onLinkGithubRepo}
                onUpdateFromGithub={this.onUpdateFromGithub}
                showVersions={this.props.showVersions}
                onDeploy={this.onDeploy}
                lastDeployTimestamp={this.props.lastDeployTimestamp}
                slackTeamId={this.props.slackTeamId}
                botName={this.props.botName}
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
  lastDeployTimestamp: React.PropTypes.string,
  slackTeamId: React.PropTypes.string,
  botName: React.PropTypes.string.isRequired
};

ReactDOM.render(
  React.createElement(BehaviorEditorLoader, BehaviorEditorConfiguration),
  document.getElementById(BehaviorEditorConfiguration.containerId)
);
