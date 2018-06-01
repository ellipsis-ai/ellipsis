import 'core-js';
import 'whatwg-fetch';
import * as React from 'react';
import * as ReactDOM from 'react-dom';
import Page, {PageRequiredProps} from '../shared_ui/page';
import BehaviorGroup from '../models/behavior_group';
import BehaviorGroupDeployment from '../models/behavior_group_deployment';
import BehaviorList from './index';
import PageNotification from '../shared_ui/page_notification';
import {DataRequest, ResponseError} from '../lib/data_request';
import ImmutableObjectUtils from '../lib/immutable_object_utils';
import TimeZoneWelcomePage from '../time_zone/time_zone_welcome_page';
import TeamTimeZoneSetter from '../time_zone/team_time_zone_setter';
import autobind from "../lib/autobind";
import {BehaviorGroupJson} from "../models/behavior_group";

export interface BehaviorListLoaderProps {
  containerId: string,
  csrfToken: string,
  behaviorGroups: Array<BehaviorGroupJson>,
  teamId: string,
  slackTeamId: string,
  teamTimeZone?: Option<string>,
  branchName: Option<string>,
  botName: string,
  feedbackContainer?: Option<HTMLElement>
}

export type PublishedBehaviorGroupLoadStatus = "loaded" | "loading" | "error";

export interface SearchResult {
  isLoading: boolean,
  error: Option<string>,
  matches: Array<BehaviorGroup>
}

export interface SearchResults {
  [searchText: string]: SearchResult | undefined
}

type State = {
  publishedBehaviorGroupLoadStatus: PublishedBehaviorGroupLoadStatus,
  publishedBehaviorGroups: Array<BehaviorGroup>,
  recentlyInstalled: Array<BehaviorGroup>,
  currentlyInstalling: Array<BehaviorGroup>,
  matchingResults: SearchResults,
  currentTeamTimeZone?: Option<string>,
  dismissedNotifications: Array<string>,
  isDeploying: boolean,
  deployError: Option<string>
}

declare var BehaviorListConfig: BehaviorListLoaderProps;

class BehaviorListLoader extends React.Component<BehaviorListLoaderProps, State> {
  constructor(props: BehaviorListLoaderProps) {
    super(props);
    autobind(this);
    this.state = {
      publishedBehaviorGroupLoadStatus: 'loading',
      publishedBehaviorGroups: [],
      recentlyInstalled: [],
      currentlyInstalling: [],
      matchingResults: {},
      currentTeamTimeZone: this.props.teamTimeZone,
      dismissedNotifications: [],
      isDeploying: false,
      deployError: null
    };
  }

  componentDidMount() {
    this.loadPublishedBehaviorGroups();
  }

  loadPublishedBehaviorGroups(): void {
    const url = jsRoutes.controllers.ApplicationController.fetchPublishedBehaviorInfo(
      this.props.teamId, this.props.branchName
    ).url;
    DataRequest
      .jsonGet(url)
      .then((groupsJson) => {
        this.setState({
          publishedBehaviorGroups: groupsJson.map(BehaviorGroup.fromJson),
          publishedBehaviorGroupLoadStatus: 'loaded'
        });
      })
      .catch(() => {
        this.setState({
          publishedBehaviorGroupLoadStatus: 'error'
        });
      });
  }

  importBehaviorGroup(groupToInstall: BehaviorGroup): void {
    const url = jsRoutes.controllers.BehaviorImportExportController.doImport().url;

    const body = {
      teamId: this.props.teamId,
      dataJson: JSON.stringify(groupToInstall)
    };

    this.setState({
      currentlyInstalling: this.state.currentlyInstalling.concat(groupToInstall)
    }, () => {
      DataRequest
        .jsonPost(url, body, this.props.csrfToken)
        .then((importedGroupJson) => {
          this.didImportPublishedGroup(groupToInstall, BehaviorGroup.fromJson(importedGroupJson));
        })
        .catch(() => {
          // TODO: Handle errors importing
        });
    });
  }

  updateBehaviorGroup(existingGroup: BehaviorGroup, updatedData: BehaviorGroup, callback?: (newGroup: BehaviorGroup) => void): void {
    const url = jsRoutes.controllers.BehaviorEditorController.save().url;

    const body = {
      dataJson: JSON.stringify(updatedData.clone({ id: existingGroup.id })),
      isReinstall: true
    };

    this.setState({
      currentlyInstalling: this.state.currentlyInstalling.concat(existingGroup)
    }, () => {
      DataRequest
        .jsonPost(url, body, this.props.csrfToken)
        .then((newGroupJson) => {
          const newGroup = BehaviorGroup.fromJson(newGroupJson);
          this.didUpdateExistingGroup(existingGroup, newGroup, callback);
        })
        .catch(() => {
          // TODO: Handle errors importing
        });
    });
  }

  didImportPublishedGroup(publishedGroup: BehaviorGroup, importedGroup: BehaviorGroup): void {
    this.setState({
      currentlyInstalling: this.state.currentlyInstalling.filter((ea) => ea !== publishedGroup),
      recentlyInstalled: this.state.recentlyInstalled.concat(importedGroup)
    });
  }

  didUpdateExistingGroup(existingGroup: BehaviorGroup, updatedGroup: BehaviorGroup, callback?: (newGroup: BehaviorGroup) => void): void {
    const index = this.state.recentlyInstalled.findIndex(ea => ea.id === updatedGroup.id);
    const newGroups = index >= 0 ?
      ImmutableObjectUtils.arrayWithNewElementAtIndex(this.state.recentlyInstalled, updatedGroup, index) :
      this.state.recentlyInstalled.concat(updatedGroup);
    this.setState({
      currentlyInstalling: this.state.currentlyInstalling.filter((ea) => ea !== existingGroup),
      recentlyInstalled: newGroups
    }, () => {
      if (callback) {
        callback(updatedGroup);
      }
    });

  }

  mergeBehaviorGroups(behaviorGroupIds: Array<string>): void {
    const url = jsRoutes.controllers.ApplicationController.mergeBehaviorGroups().url;
    const body = { behaviorGroupIds: behaviorGroupIds };
    DataRequest
      .jsonPost(url, body, this.props.csrfToken)
      .then(() => {
        window.location.reload();
      })
      .catch(() => {
        // TODO: Error handling
      });
  }

  deleteBehaviorGroups(behaviorGroupIds: Array<string>): void {
    const url = jsRoutes.controllers.ApplicationController.deleteBehaviorGroups().url;
    const body = { behaviorGroupIds: behaviorGroupIds };
    DataRequest
      .jsonPost(url, body, this.props.csrfToken)
      .then(() => {
        window.location.reload();
      })
      .catch(() => {
        // TODO: Error handling
      });
  }

  deployBehaviorGroup(behaviorGroupId: string): void {
    this.setState({
      isDeploying: true,
      deployError: null
    }, () => {
      DataRequest.jsonPost(
        jsRoutes.controllers.BehaviorEditorController.deploy().url,
        { behaviorGroupId: behaviorGroupId },
        this.props.csrfToken
      ).then((deploymentJson) => {
        if (deploymentJson.id) {
          this.didDeployBehaviorGroup(behaviorGroupId, BehaviorGroupDeployment.fromJson(deploymentJson));
        } else {
          this.onDeployError();
        }
      }).catch((error) => {
        this.onDeployError(error);
      });
    });
  }

  didDeployBehaviorGroup(groupId: string, deployment: BehaviorGroupDeployment): void {
    const updated = this.state.recentlyInstalled.map((ea) => {
      if (ea.id === groupId) {
        return ea.clone({ deployment: deployment });
      } else {
        return ea;
      }
    });
    this.setState({
      isDeploying: false,
      recentlyInstalled: updated
    });
  }

  onDeployError(error?: ResponseError): void {
    const errorMessage = error && error.body || "Skill could not be deployed. An unknown error occurred.";
    this.setState({
      isDeploying: false,
      deployError: errorMessage
    });
  }

  updateMatchResultsFor(queryString: string, newResult: SearchResult, callback?: () => void): void {
    const newResults: SearchResults = {};
    newResults[queryString] = newResult;
    this.setState((prevState) => {
      return {
        matchingResults: Object.assign({}, prevState.matchingResults, newResults)
      };
    }, callback);
  }

  getSearchResults(queryString: string): void {
    const trimmed = queryString.trim();
    if (trimmed) {
      const existingResult = this.state.matchingResults[trimmed];
      if (existingResult && (existingResult.isLoading || !existingResult.error)) {
        return;
      }
      const loadingResult: SearchResult = {
        isLoading: true,
        error: null,
        matches: []
      };
      this.updateMatchResultsFor(queryString, loadingResult, () => {
        const url = jsRoutes.controllers.ApplicationController.findBehaviorGroupsMatching(queryString, this.props.branchName, this.props.teamId).url;
        DataRequest
          .jsonGet(url)
          .then((matchingGroupsJson) => {
            const loadedResult: SearchResult = {
              isLoading: false,
              error: null,
              matches: matchingGroupsJson.map(BehaviorGroup.fromJson)
            };
            this.updateMatchResultsFor(queryString, loadedResult);
          })
          .catch(() => {
            const errorResult: SearchResult = {
              isLoading: false,
              error: `An error occurred while searching for “${queryString}”`,
              matches: []
            };
            this.updateMatchResultsFor(queryString, errorResult);
          });
      });
    }
  }

  dismissNotification(notificationName: string): void {
    this.setState({
      dismissedNotifications: this.state.dismissedNotifications.concat(notificationName)
    });
  }

  hasDismissedNotification(notificationName: string): boolean {
    return this.state.dismissedNotifications.includes(notificationName);
  }

  onSaveTimeZone(newTzId: string, newTzName: string): void {
    this.setState({
      currentTeamTimeZone: newTzName
    });
  }

  renderTimeZoneSetNotification() {
    return !this.props.teamTimeZone && this.state.currentTeamTimeZone ? (
      <PageNotification
        name="TIME_ZONE_SET"
        content={`Your team’s time zone has been set to ${this.state.currentTeamTimeZone}.`}
        onDismiss={this.dismissNotification}
        isDismissed={this.hasDismissedNotification("TIME_ZONE_SET")}
      />
    ) : null;
  }

  renderPage<P extends PageRequiredProps>(pageProps: PageRequiredProps): React.ReactElement<P> {
    if (this.state.currentTeamTimeZone) {
      return (
        <BehaviorList
          onLoadPublishedBehaviorGroups={this.loadPublishedBehaviorGroups}
          onBehaviorGroupImport={this.importBehaviorGroup}
          onBehaviorGroupUpdate={this.updateBehaviorGroup}
          onMergeBehaviorGroups={this.mergeBehaviorGroups}
          onDeleteBehaviorGroups={this.deleteBehaviorGroups}
          onBehaviorGroupDeploy={this.deployBehaviorGroup}
          onSearch={this.getSearchResults}
          localBehaviorGroups={this.props.behaviorGroups.map(BehaviorGroup.fromJson)}
          publishedBehaviorGroups={this.state.publishedBehaviorGroups}
          recentlyInstalled={this.state.recentlyInstalled}
          currentlyInstalling={this.state.currentlyInstalling}
          isDeploying={this.state.isDeploying}
          deployError={this.state.deployError}
          publishedBehaviorGroupLoadStatus={this.state.publishedBehaviorGroupLoadStatus}
          matchingResults={this.state.matchingResults}
          teamId={this.props.teamId}
          slackTeamId={this.props.slackTeamId}
          botName={this.props.botName}
          notification={this.renderTimeZoneSetNotification()}
          {...pageProps}
        />
      );
    } else {
      return (
        <TimeZoneWelcomePage {...pageProps}>
          <TeamTimeZoneSetter
            csrfToken={this.props.csrfToken}
            teamId={this.props.teamId}
            onSave={this.onSaveTimeZone}
          />
        </TimeZoneWelcomePage>
      );
    }
  }

  render() {
    return (
      <Page csrfToken={this.props.csrfToken}
        feedbackContainer={this.props.feedbackContainer}
        onRender={this.renderPage}
      />
    );
  }
}

if (typeof BehaviorListConfig !== "undefined") {
  const container = document.getElementById(BehaviorListConfig.containerId);
  if (container) {
    ReactDOM.render((
      <BehaviorListLoader {...BehaviorListConfig} />
    ), container);
  }
}

export default BehaviorListLoader;
