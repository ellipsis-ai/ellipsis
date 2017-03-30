define(function(require) {
  var React = require('react'),
    BehaviorGroup = require('../models/behavior_group'),
    BehaviorList = require('./index'),
    Collapsible = require('../shared_ui/collapsible'),
    DataRequest = require('../lib/data_request'),
    ImmutableObjectUtils = require('../lib/immutable_object_utils'),
    TimeZoneSetter = require('../time_zone_setter/index');

  return React.createClass({
    displayName: 'App',
    propTypes: {
      csrfToken: React.PropTypes.string.isRequired,
      behaviorGroups: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
      teamId: React.PropTypes.string.isRequired,
      slackTeamId: React.PropTypes.string.isRequired,
      teamTimeZone: React.PropTypes.string,
      branchName: React.PropTypes.string
    },

    componentDidMount: function() {
      this.loadPublishedBehaviorGroups();
    },

    getInitialState: function() {
      return {
        behaviorGroups: this.props.behaviorGroups,
        publishedBehaviorGroupLoadStatus: 'loading',
        publishedBehaviorGroups: [],
        recentlyInstalled: [],
        matchingResults: [],
        currentSearchText: "",
        isLoadingMatchingResults: false,
        currentTeamTimeZone: this.props.teamTimeZone,
        isSavingTeamTimeZone: false,
        errorSavingTeamTimeZone: null
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

      DataRequest
        .jsonPost(url, body, this.props.csrfToken)
        .then((installedGroup) => {
          this.setState({
            recentlyInstalled: this.state.recentlyInstalled.concat(installedGroup)
          });
        })
        .catch(() => {
          // TODO: Handle errors importing
        });
    },

    updateBehaviorGroup: function(existingGroup, updatedData) {
      const url = jsRoutes.controllers.ApplicationController.updateBehaviorGroup().url;

      const body = {
        dataJson: JSON.stringify(updatedData.clone({ id: existingGroup.id }))
      };

      DataRequest
        .jsonPost(url, body, this.props.csrfToken)
        .then((installedGroup) => {
          const index = this.state.behaviorGroups.findIndex(ea => ea.id === installedGroup.id);
          const newGroups = ImmutableObjectUtils.arrayWithNewElementAtIndex(this.state.behaviorGroups, installedGroup, index);
          this.setState({
            behaviorGroups: newGroups
          });
        })
        .catch(() => {
          // TODO: Handle errors importing
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

    setTimeZone: function(newTz, displayName) {
      this.setState({
        isSavingTeamTimeZone: true,
        errorSavingTeamTimeZone: null
      }, () => {
        const url = jsRoutes.controllers.ApplicationController.setTeamTimeZone().url;
        DataRequest
          .jsonPost(url, {
            tzName: newTz,
            teamId: this.props.teamId
          }, this.props.csrfToken)
          .then((json) => {
            if (json.newTz) {
              this.setState({
                currentTeamTimeZone: displayName,
                isSavingTeamTimeZone: false
              });
            } else {
              throw new Error(json.message || "");
            }
          })
          .catch((err) => {
            this.setState({
              isSavingTeamTimeZone: false,
              errorSavingTeamTimeZone: `An error occurred while saving${err.message ? ` (${err.message})` : ""}. Please try again.`
            });
          });
      });
    },

    render: function() {
      if (this.state.currentTeamTimeZone) {
        return (
          <div>
            <Collapsible revealWhen={!this.props.teamTimeZone} animateInitialRender={true}>
              <div className="bg-blue-light pvm border-bottom-thick border-blue">
                <div className="container container-c">
                  <div className="mhl">
                    Your team’s time zone has been set to {this.state.currentTeamTimeZone}.
                  </div>
                </div>
              </div>
            </Collapsible>
            <BehaviorList
              onLoadPublishedBehaviorGroups={this.loadPublishedBehaviorGroups}
              onBehaviorGroupImport={this.importBehaviorGroup}
              onBehaviorGroupUpdate={this.updateBehaviorGroup}
              onMergeBehaviorGroups={this.mergeBehaviorGroups}
              onDeleteBehaviorGroups={this.deleteBehaviorGroups}
              onSearch={this.getSearchResults}
              localBehaviorGroups={this.state.behaviorGroups.map(BehaviorGroup.fromJson)}
              publishedBehaviorGroups={this.state.publishedBehaviorGroups.map(BehaviorGroup.fromJson)}
              recentlyInstalled={this.state.recentlyInstalled.map(BehaviorGroup.fromJson)}
              matchingResults={this.state.matchingResults.map(BehaviorGroup.fromJson)}
              currentSearchText={this.state.currentSearchText}
              isLoadingMatchingResults={this.state.isLoadingMatchingResults}
              publishedBehaviorGroupLoadStatus={this.state.publishedBehaviorGroupLoadStatus}
              teamId={this.props.teamId}
              slackTeamId={this.props.slackTeamId}
            />
          </div>
        );
      } else {
        return (
          <TimeZoneSetter
            onSetTimeZone={this.setTimeZone}
            isSaving={this.state.isSavingTeamTimeZone}
            error={this.state.errorSavingTeamTimeZone}
          />
        );
      }
    }
  });
});
