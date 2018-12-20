import 'core-js';
import * as React from 'react';
import * as ReactDOM from 'react-dom';
import BrowserUtils from '../lib/browser_utils';
import BehaviorEditor from './index';
import BehaviorGroup, {BehaviorGroupJson} from '../models/behavior_group';
import BehaviorGroupDeployment, {BehaviorGroupDeploymentJson} from '../models/behavior_group_deployment';
import BehaviorResponseType, {BehaviorResponseTypeJson} from '../models/behavior_response_type';
import ParamType, {ParamTypeJson} from '../models/param_type';
import {AWSConfigRef, AWSConfigRefJson} from '../models/aws';
import {OAuthApiJson, OAuthApplicationRef, OAuthApplicationRefJson} from '../models/oauth';
import {SimpleTokenApiRef, SimpleTokenApiRefJson} from '../models/simple_token';
import LinkedGithubRepo, {LinkedGitHubRepoJson} from '../models/linked_github_repo';
import autobind from '../lib/autobind';
import Page from '../shared_ui/page';
import {DataRequest} from '../lib/data_request';
import {SavedAnswer} from "./user_input_configuration";
import {EnvironmentVariableData} from "../settings/environment_variables/loader";
import {Timestamp} from "../lib/formatter";
import {GithubFetchError} from "../models/github/github_fetch_error";

interface Props {
  containerId: string
  csrfToken: string
  group: BehaviorGroupJson
  selectedId?: Option<string>
  builtinParamTypes: Array<ParamTypeJson>
  envVariables: Array<EnvironmentVariableData>
  savedAnswers: Array<SavedAnswer>
  awsConfigs: Array<AWSConfigRefJson>
  oauthApplications: Array<OAuthApplicationRefJson>
  oauthApis: Array<OAuthApiJson>
  simpleTokenApis: Array<SimpleTokenApiRefJson>
  linkedOAuthApplicationIds: Array<string>
  userId: string
  isAdmin: boolean
  isLinkedToGithub: boolean
  showVersions?: boolean
  lastDeployTimestamp?: Option<Timestamp>
  slackTeamId?: Option<string>
  botName: string
  possibleResponseTypes: Array<BehaviorResponseTypeJson>
}

interface State {
  awsConfigs: Array<AWSConfigRef>
  oauthApplications: Array<OAuthApplicationRef>
  simpleTokenApis: Array<SimpleTokenApiRef>
  linkedGithubRepo: Option<LinkedGithubRepo>
  group: BehaviorGroup,
  builtinParamTypes: Array<ParamType>
  selectedId: Option<string>
  savedAnswers: Array<SavedAnswer>
  onLoad: Option<() => void>
}

interface LinkToGithubData {
  behaviorGroupId: string
  owner: string
  repo: string
  currentBranch?: Option<string>
}

export interface UpdateFromGithubSuccessData {
  data: BehaviorGroupJson
  errors: undefined
}

interface UpdateFromGithubErrorData {
  data: undefined
  errors: GithubFetchError
}

type UpdateFromGithubResponseData = UpdateFromGithubSuccessData | UpdateFromGithubErrorData

declare var BehaviorEditorConfiguration: Props;

class BehaviorEditorLoader extends React.Component<Props, State> {
        constructor(props: Props) {
          super(props);
          autobind(this);
          const group = BehaviorGroup.fromJson(this.props.group);
          const selectedId = this.props.selectedId || this.fallbackSelectedIdFor(group);
          this.state = {
            awsConfigs: this.props.awsConfigs.map(AWSConfigRef.fromJson),
            oauthApplications: this.props.oauthApplications.map(OAuthApplicationRef.fromJson),
            simpleTokenApis: this.props.simpleTokenApis.map(SimpleTokenApiRef.fromJson),
            linkedGithubRepo: group.linkedGithubRepo,
            group: group,
            builtinParamTypes: this.props.builtinParamTypes.map(ParamType.fromJson),
            selectedId: selectedId,
            savedAnswers: this.props.savedAnswers,
            onLoad: null
          };
          if (group.id && selectedId) {
            BrowserUtils.replaceURL(jsRoutes.controllers.BehaviorEditorController.edit(group.id, selectedId, this.props.showVersions).url);
          }
        }

        onLinkGithubRepo(owner: string, repo: string, branch: Option<string>, callback?: () => void): void {
          const url = jsRoutes.controllers.BehaviorEditorController.linkToGithubRepo().url;
          const groupId = this.state.group.id;
          if (groupId) {
            const params: LinkToGithubData = {
              behaviorGroupId: groupId,
              owner: owner,
              repo: repo
            };
            if (branch) {
              params.currentBranch = branch;
            }
            DataRequest.jsonPost(url, params, this.props.csrfToken)
              .then((linkedData: LinkedGitHubRepoJson) => {
                this.setState({ linkedGithubRepo: LinkedGithubRepo.fromJson(linkedData) }, callback);
              });
          }
        }

        isUpdateFromGithubErrorData(json: UpdateFromGithubResponseData): json is UpdateFromGithubErrorData {
          return typeof json === 'object' && typeof json.errors !== 'undefined';
        }

        onUpdateFromGithub(
          owner: string,
          repo: string,
          branch: string,
          callback: (json: UpdateFromGithubSuccessData) => void,
          onError: (branch: string, error?: Option<GithubFetchError>) => void
        ): void {
          DataRequest.jsonPost(
            jsRoutes.controllers.BehaviorEditorController.updateFromGithub().url, {
              behaviorGroupId: this.state.group.id,
              owner: owner,
              repo: repo,
              branch: branch
            },
            this.props.csrfToken
          ).then((json: UpdateFromGithubResponseData) => {
            this.setState({
              linkedGithubRepo: new LinkedGithubRepo(owner, repo, branch)
            }, () => {
              if (this.isUpdateFromGithubErrorData(json)) {
                onError(branch, json.errors);
              } else {
                callback(json);
              }
            });
          }).catch(() => {
            onError(branch, null);
          });
        }

        onSave(newProps: { group: BehaviorGroup, onLoad?: Option<() => void> }): void {
          this.setState({
            group: newProps.group,
            onLoad: newProps.onLoad
          });
        }

        onDeploy(deploymentProps: BehaviorGroupDeploymentJson, callback?: () => void): void {
          this.setState({
            group: this.state.group.clone({ deployment: BehaviorGroupDeployment.fromJson(deploymentProps) }),
            onLoad: null
          }, callback);
        }

        fallbackSelectedIdFor(group: BehaviorGroup): Option<string> {
          const isSimpleBehaviorGroup = !group.name && !group.description && group.behaviorVersions.length === 1;
          if (isSimpleBehaviorGroup) {
            return group.behaviorVersions[0].behaviorId;
          } else {
            return null;
          }
        }

        getSavedAnswers(): Array<SavedAnswer> {
          return (this.state && this.state.savedAnswers) ? this.state.savedAnswers : [];
        }

        resetSavedAnswerForInput(inputId: string, numAnswersDeleted: number): void {
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
            <Page csrfToken={this.props.csrfToken}
              onRender={(pageProps) => (
              <BehaviorEditor
                group={this.state.group}
                selectedId={this.state.selectedId}
                csrfToken={this.props.csrfToken}
                builtinParamTypes={this.state.builtinParamTypes}
                envVariables={this.props.envVariables}
                awsConfigs={this.state.awsConfigs}
                oauthApplications={this.state.oauthApplications}
                oauthApis={this.props.oauthApis}
                simpleTokenApis={this.state.simpleTokenApis}
                linkedOAuthApplicationIds={this.props.linkedOAuthApplicationIds}
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
                possibleResponseTypes={this.props.possibleResponseTypes.map(BehaviorResponseType.fromProps)}
                {...pageProps}
              />
              )}
            />
          );
        }
}

const container = document.getElementById(BehaviorEditorConfiguration.containerId);
if (container) {
  ReactDOM.render((
    <BehaviorEditorLoader {...BehaviorEditorConfiguration} />
  ), container);
}
