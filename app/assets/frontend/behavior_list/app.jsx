import * as React from 'react';
import BehaviorGroup from '../models/behavior_group';
import BehaviorList from './index';
import PageNotification from '../shared_ui/page_notification';
import DataRequest from '../lib/data_request';
import ImmutableObjectUtils from '../lib/immutable_object_utils';
import TimeZoneWelcomePage from '../time_zone/time_zone_welcome_page';
import TeamTimeZoneSetter from '../time_zone/team_time_zone_setter';
import Page from '../shared_ui/page';

const BehaviorListApp = React.createClass({
    propTypes: Object.assign({}, Page.requiredPropTypes, {
      csrfToken: React.PropTypes.string.isRequired,
      behaviorGroups: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
      teamId: React.PropTypes.string.isRequired,
      slackTeamId: React.PropTypes.string.isRequired,
      teamTimeZone: React.PropTypes.string,
      branchName: React.PropTypes.string
    }),

    getDefaultProps: function() {
      return Page.requiredPropDefaults();
    },

    componentDidMount: function() {
      this.loadPublishedBehaviorGroups();
    },

    getInitialState: function() {
      return {
        publishedBehaviorGroupLoadStatus: 'loading',
        publishedBehaviorGroups: [],
        recentlyInstalled: [],
        currentlyInstalling: [],
        matchingResults: [],
        currentSearchText: "",
        isLoadingMatchingResults: false,
        currentTeamTimeZone: this.props.teamTimeZone,
        dismissedNotifications: []
      };
    },

    loadPublishedBehaviorGroups: function() {
      const url = jsRoutes.controllers.ApplicationController.fetchPublishedBehaviorInfo(
        this.props.teamId, this.props.branchName
      ).url;
      DataRequest
        .jsonGet(url)
        .then((json) => {
          this.setState({
            publishedBehaviorGroups: json,
            publishedBehaviorGroupLoadStatus: 'loaded'
          });
        })
        .catch(() => {
          this.setState({
            publishedBehaviorGroupLoadStatus: 'error'
          });
        });
    },

    importBehaviorGroup: function(groupToInstall) {
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
          .then((importedGroup) => this.didImportPublishedGroup(groupToInstall, importedGroup))
          .catch(() => {
            // TODO: Handle errors importing
          });
      });
    },

    updateBehaviorGroup: function(existingGroup, updatedData) {
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
          .then((newGroup) => this.didUpdateExistingGroup(existingGroup, newGroup))
          .catch(() => {
            // TODO: Handle errors importing
          });
      });
    },

    didImportPublishedGroup: function(publishedGroup, importedGroup) {
      this.setState({
        currentlyInstalling: this.state.currentlyInstalling.filter((ea) => ea !== publishedGroup),
        recentlyInstalled: this.state.recentlyInstalled.concat(importedGroup)
      });
    },

    didUpdateExistingGroup: function(existingGroup, updatedGroup) {
      const index = this.state.recentlyInstalled.findIndex(ea => ea.id === updatedGroup.id);
      const newGroups = index >= 0 ?
        ImmutableObjectUtils.arrayWithNewElementAtIndex(this.state.recentlyInstalled, updatedGroup, index) :
        this.state.recentlyInstalled.concat(updatedGroup);
      this.setState({
        currentlyInstalling: this.state.currentlyInstalling.filter((ea) => ea !== existingGroup),
        recentlyInstalled: newGroups
      });
    },

    mergeBehaviorGroups: function(behaviorGroupIds) {
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
    },

    deleteBehaviorGroups: function(behaviorGroupIds) {
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
    },

    getSearchResults: function(queryString) {
      const trimmed = queryString.trim();
      if (trimmed) {
        this.setState({
          isLoadingMatchingResults: true
        });
        const url = jsRoutes.controllers.ApplicationController.findBehaviorGroupsMatching(queryString, this.props.branchName, this.props.teamId).url;
        DataRequest
          .jsonGet(url)
          .then((results) => {
            this.setState({
              isLoadingMatchingResults: false,
              matchingResults: results,
              currentSearchText: trimmed
            });
          })
          .catch(() => {
            // TODO: no really, error handling
          });
      } else {
        this.setState({
          matchingResults: [],
          currentSearchText: ""
        });
      }
    },

    dismissNotification: function(notificationName) {
      this.setState({
        dismissedNotifications: this.state.dismissedNotifications.concat(notificationName)
      });
    },

    hasDismissedNotification: function(notificationName) {
      return this.state.dismissedNotifications.includes(notificationName);
    },

    shouldNotifyTimeZone: function() {
      return !this.props.teamTimeZone && !!this.state.currentTeamTimeZone;
    },

    onSaveTimeZone: function(newTzId, newTzName) {
      this.setState({
        currentTeamTimeZone: newTzName
      });
    },

    render: function() {
      if (this.state.currentTeamTimeZone) {
        return (
          <div>
            {this.shouldNotifyTimeZone() ? (
              <PageNotification
              name="TIME_ZONE_SET"
              content={`Your team’s time zone has been set to ${this.state.currentTeamTimeZone}.`}
              onDismiss={this.dismissNotification}
              isDismissed={this.hasDismissedNotification("TIME_ZONE_SET")}
              />
            ) : null}
            <BehaviorList
              onLoadPublishedBehaviorGroups={this.loadPublishedBehaviorGroups}
              onBehaviorGroupImport={this.importBehaviorGroup}
              onBehaviorGroupUpdate={this.updateBehaviorGroup}
              onMergeBehaviorGroups={this.mergeBehaviorGroups}
              onDeleteBehaviorGroups={this.deleteBehaviorGroups}
              onSearch={this.getSearchResults}
              localBehaviorGroups={this.props.behaviorGroups.map(BehaviorGroup.fromJson)}
              publishedBehaviorGroups={this.state.publishedBehaviorGroups.map(BehaviorGroup.fromJson)}
              recentlyInstalled={this.state.recentlyInstalled.map(BehaviorGroup.fromJson)}
              currentlyInstalling={this.state.currentlyInstalling}
              matchingResults={this.state.matchingResults.map(BehaviorGroup.fromJson)}
              currentSearchText={this.state.currentSearchText}
              isLoadingMatchingResults={this.state.isLoadingMatchingResults}
              publishedBehaviorGroupLoadStatus={this.state.publishedBehaviorGroupLoadStatus}
              teamId={this.props.teamId}
              slackTeamId={this.props.slackTeamId}
              activePanelName={this.props.activePanelName}
              activePanelIsModal={this.props.activePanelIsModal}
              onToggleActivePanel={this.props.onToggleActivePanel}
              onClearActivePanel={this.props.onClearActivePanel}
              onRenderFooter={this.props.onRenderFooter}
              onRenderNavItems={this.props.onRenderNavItems}
              onRenderNavActions={this.props.onRenderNavActions}
              footerHeight={this.props.footerHeight}
            />
          </div>
        );
      } else {
        return (
          <TimeZoneWelcomePage>
            <TeamTimeZoneSetter
              csrfToken={this.props.csrfToken}
              teamId={this.props.teamId}
              onSave={this.onSaveTimeZone}
            />
            {this.props.onRenderFooter()}
          </TimeZoneWelcomePage>
        );
      }
    }
});

export default BehaviorListApp;
