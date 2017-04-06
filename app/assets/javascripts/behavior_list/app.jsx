define(function(require) {
  var React = require('react'),
    BehaviorGroup = require('../models/behavior_group'),
    BehaviorList = require('./index'),
    DataRequest = require('../lib/data_request'),
    ImmutableObjectUtils = require('../lib/immutable_object_utils');

  return React.createClass({
    displayName: 'BehaviorListApp',
    propTypes: {
      csrfToken: React.PropTypes.string.isRequired,
      behaviorGroups: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
      teamId: React.PropTypes.string.isRequired,
      slackTeamId: React.PropTypes.string.isRequired,
      branchName: React.PropTypes.string
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
        isLoadingMatchingResults: false
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

    render: function() {
      return (
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
        />
      );
    }
  });
});
