define(function(require) {
  const React = require('react'),
    BrowserUtils = require('../lib/browser_utils'),
    Collapsible = require('../shared_ui/collapsible'),
    ConfirmActionPanel = require('../panels/confirm_action'),
    DynamicLabelButton = require('../form/dynamic_label_button'),
    FixedFooter = require('../shared_ui/fixed_footer'),
    ModalScrim = require('../shared_ui/modal_scrim'),
    PageWithPanels = require('../shared_ui/page_with_panels'),
    BehaviorGroup = require('../models/behavior_group'),
    ScheduledAction = require('../models/scheduled_action'),
    ScheduleChannel = require('../models/schedule_channel'),
    ScheduledItem = require('./scheduled_item'),
    ScheduledItemEditor = require('./scheduled_item_editor'),
    ScheduledItemTitle = require('./scheduled_item_title'),
    Sort = require('../lib/sort');

  const Scheduling = React.createClass({
    displayName: 'Scheduling',
    propTypes: Object.assign(PageWithPanels.requiredPropTypes(), {
      scheduledActions: React.PropTypes.arrayOf(React.PropTypes.instanceOf(ScheduledAction)).isRequired,
      channelList: React.PropTypes.arrayOf(React.PropTypes.instanceOf(ScheduleChannel)).isRequired,
      behaviorGroups: React.PropTypes.arrayOf(React.PropTypes.instanceOf(BehaviorGroup)).isRequired,
      teamTimeZone: React.PropTypes.string,
      teamTimeZoneName: React.PropTypes.string,
      slackUserId: React.PropTypes.string,
      slackBotUserId: React.PropTypes.string,
      onSave: React.PropTypes.func.isRequired,
      isSaving: React.PropTypes.bool.isRequired,
      onDelete: React.PropTypes.func.isRequired,
      isDeleting: React.PropTypes.bool.isRequired,
      error: React.PropTypes.string,
      onClearErrors: React.PropTypes.func.isRequired,
      justSavedAction: React.PropTypes.instanceOf(ScheduledAction),
      selectedScheduleId: React.PropTypes.string,
      newAction: React.PropTypes.bool
    }),

    getInitialState: function() {
      const selectedItem = this.getDefaultSelectedItem();
      return {
        filterChannel: null,
        selectedItem: selectedItem,
        justSaved: false,
        justDeleted: false,
        isEditing: Boolean(selectedItem)
      };
    },

    componentWillReceiveProps(nextProps) {
      const justSaved = this.props.isSaving && !nextProps.isSaving;
      const justDeleted = this.props.isDeleting && !nextProps.isDeleting;
      const newAction = justSaved ? nextProps.justSavedAction : null;
      if (justSaved || justDeleted) {
        this.props.onClearActivePanel();
      }
      if (justSaved) {
        this.setState({
          filterChannel: null,
          selectedItem: newAction || this.state.selectedItem,
          justSaved: !nextProps.error,
          justDeleted: false,
          isEditing: Boolean(nextProps.error)
        });
      } else if (justDeleted) {
        this.setState({
          filterChannel: null,
          selectedItem: nextProps.error ? this.state.selectedItem : null,
          justSaved: false,
          justDeleted: !nextProps.error,
          isEditing: Boolean(nextProps.error)
        });
      }
    },

    componentDidUpdate(prevProps, prevState) {
      if (prevState.isEditing !== this.state.isEditing) {
        window.scrollTo(0, 0);

        const explicitTeamId = BrowserUtils.hasQueryParam("teamId") ? this.props.teamId : null;
        BrowserUtils.replaceURL(this.getCorrectedURL(explicitTeamId));
      }
    },

    getDefaultSelectedItem: function() {
      if (this.props.selectedScheduleId) {
        return this.props.scheduledActions.find((ea) => ea.id === this.props.selectedScheduleId);
      } else if (this.props.newAction) {
        return this.createNewSchedule();
      } else {
        return null;
      }
    },

    getCorrectedURL: function(explicitTeamId) {
      if (this.state.isEditing && this.state.selectedItem && !this.state.selectedItem.isNew()) {
        return jsRoutes.controllers.ScheduledActionsController.index(this.state.selectedItem.id, null, explicitTeamId).url;
      } else if (this.state.isEditing && this.state.selectedItem) {
        return jsRoutes.controllers.ScheduledActionsController.index(null, true, explicitTeamId).url;
      } else {
        return jsRoutes.controllers.ScheduledActionsController.index(null, null, explicitTeamId).url;
      }
    },

    isEditing: function() {
      return this.state.isEditing;
    },

    getSelectedItem: function() {
      return this.state.selectedItem;
    },

    updateSelectedItem: function(newItem, optionalCallback) {
      this.setState({
        selectedItem: newItem
      }, optionalCallback);
    },

    getScheduleByChannel: function() {
      const groupsByName = {};
      this.props.scheduledActions.forEach((action) => {
        const channel = this.props.channelList.find((ea) => ea.id === action.channel);
        const channelName = channel ? channel.getFormattedName() : "Unknown";
        if (!channel || channel.members.includes(this.props.slackUserId)) {
          const group = groupsByName[channelName] || {
            channelName: channelName,
            channelId: channel.id,
            excludesBot: this.props.slackBotUserId && !channel.userCanAccess(this.props.slackBotUserId),
            actions: []
          };
          group.actions.push(action);
          groupsByName[channelName] = group;
        }
      });
      const channelNames = Object.keys(groupsByName);
      const sortedNames = Sort.arrayAlphabeticalBy(channelNames, (ea) => ea);
      return sortedNames.map((channelName) => groupsByName[channelName]);
    },

    shouldShowChannel: function(channelName) {
      return !this.state.filterChannel || this.state.filterChannel === channelName;
    },

    toggleFilter: function(channelName) {
      const newState = {};
      if (this.filterActiveFor(channelName)) {
        newState.filterChannel = null;
      } else {
        newState.filterChannel = channelName;
      }
      this.setState(newState);
    },

    clearFilters: function() {
      this.setState({
        filterChannel: null
      });
    },

    filterActiveFor: function(channelName) {
      return this.state.filterChannel === channelName;
    },

    toggleEditor: function(action) {
      this.props.onClearErrors();
      this.setState({
        selectedItem: action,
        justSaved: false,
        justDeleted: false,
        isEditing: true
      });
    },

    createNewSchedule: function() {
      return ScheduledAction.newWithDefaults(this.props.teamTimeZone, this.props.teamTimeZoneName);
    },

    addNewItem: function() {
      this.props.onClearErrors();
      this.setState({
        selectedItem: this.createNewSchedule(),
        justSaved: false,
        justDeleted: false,
        isEditing: true
      });
    },

    cancelEditor: function() {
      this.setState({
        selectedItem: null,
        isEditing: false
      });
    },

    saveSelectedItem: function() {
      this.props.onSave(this.getSelectedItem());
    },

    confirmDeleteItem: function() {
      this.props.onToggleActivePanel("confirmDelete", true);
    },

    deleteSelectedItem: function() {
      this.props.onDelete(this.getSelectedItem());
    },

    undoConfirmDelete: function() {
      this.props.onClearActivePanel();
    },

    selectedItemHasChanges: function() {
      const selected = this.getSelectedItem();
      if (!selected) {
        return false;
      }
      if (selected.isNew()) {
        return true;
      }
      const original = this.props.scheduledActions.find((ea) => ea.id === selected.id);
      if (!original) {
        return true;
      }
      return !original.isIdenticalTo(selected);
    },

    renderSubheading: function() {
      if (this.isEditing()) {
        const selectedItem = this.getSelectedItem();
        return (
          <span className="fade-in">
            <span className="mhs">→</span>
            <span className="mhs">{selectedItem && !selectedItem.isNew() ? "Edit schedule" : "New schedule"}</span>
          </span>
        );
      }
    },

    shouldShowActions: function() {
      return this.isEditing() && !this.props.activePanelName;
    },

    hasActiveRequest: function() {
      return this.props.isSaving || this.props.isDeleting;
    },

    renderSidebar: function(groups) {
      return (
        <div>
          <div className="phxl mobile-phl mbs">
            <h5 className="display-inline-block prm">Filter by channel</h5>
            <span>
              <button type="button"
                className="button-s button-shrink"
                disabled={!this.state.filterChannel}
                onClick={this.clearFilters}
              >
                Clear
              </button>
            </span>
          </div>

          <div>
            {this.renderFilterList(groups)}
          </div>
        </div>
      );
    },

    renderFilterList: function(groups) {
      return groups.map((group) => (
        <button
          className={`button-block width-full phxl mobile-phl pvxs mvxs ${
            this.filterActiveFor(group.channelName) ? "bg-blue type-white " : "type-link "
            }`}
          key={`filter-${group.channelName}`}
          onClick={() => this.toggleFilter(group.channelName)}
        >
          {group.channelName}
        </button>
      ));
    },

    renderScheduleList: function(groups) {
      return groups.map((group) => (
        <Collapsible key={`group-${group.channelName}`} revealWhen={this.shouldShowChannel(group.channelName)}>
          <div className="pvl">
            <div className="phxxxl mobile-phl">
              <h4>
                <span className="mrs">{group.channelName}</span>
                {group.excludesBot ? (
                  <span className="type-s type-pink type-bold type-italic">
                    — Warning: Ellipsis must be invited to this channel for any scheduled action to run
                  </span>
                ) : null}
              </h4>
            </div>

            <div>
              {group.actions.map((action) => (
                <div className="pvxl phxxxl mobile-phl border-top" key={`${action.type}-${action.id}`}>
                  <ScheduledItem scheduledAction={action} behaviorGroups={this.props.behaviorGroups} onClick={this.toggleEditor} />
                </div>
              ))}
            </div>
          </div>
        </Collapsible>
      ));
    },

    render: function() {
      const groups = this.getScheduleByChannel();
      const selectedItem = this.getSelectedItem();
      const selectedItemIsValid = Boolean(selectedItem && selectedItem.isValid());
      const selectedItemIsNew = Boolean(selectedItem && selectedItem.isNew());
      return (
        <div>
          <div className="bg-light">
            <div className="container container-wide pvxl">
              <div className="columns columns-elastic mobile-columns-float">
                <div className="column column-expand align-b">
                  <h3 className="mvn type-weak display-ellipsis mts">
                    <span className="mrs">Scheduling</span>
                    {this.renderSubheading()}
                  </h3>
                </div>
                <div className="column column-shrink mobile-ptm">
                  <Collapsible revealWhen={!this.isEditing()}>
                    <button type="button" className="button-shrink" onClick={this.addNewItem}>Schedule something new</button>
                  </Collapsible>
                </div>
              </div>
            </div>
          </div>
          <Collapsible revealWhen={!this.isEditing()}>
            <div className="flex-columns">
              <div className="flex-column flex-column-left container container-wide phn">
                <div className="columns">
                  <div className="column column-one-quarter mobile-column-full ptxl phn">
                    {this.renderSidebar(groups)}
                  </div>
                  <div className="column column-three-quarters mobile-column-full bg-white border-radius-bottom-left ptxl pbxxxxl">
                    {this.renderScheduleList(groups)}
                  </div>
                </div>
              </div>
            </div>
          </Collapsible>
          <Collapsible revealWhen={this.isEditing()}>
            <ScheduledItemEditor
              scheduledAction={selectedItem}
              channelList={this.props.channelList}
              behaviorGroups={this.props.behaviorGroups}
              onChange={this.updateSelectedItem}
              teamTimeZone={this.props.teamTimeZone || "America/New_York"}
              teamTimeZoneName={this.props.teamTimeZoneName || "Eastern Time"}
              slackUserId={this.props.slackUserId || ""}
              slackBotUserId={this.props.slackBotUserId || ""}
            />
          </Collapsible>

          <ModalScrim isActive={this.props.activePanelIsModal} onClick={this.props.onClearActivePanel} />
          <FixedFooter ref="footer" className="bg-white">
            <Collapsible revealWhen={this.props.activePanelName === 'confirmDelete'}>
              <ConfirmActionPanel
                confirmText="Unschedule"
                confirmingText="Unscheduling…"
                isConfirming={this.props.isDeleting}
                onConfirmClick={this.deleteSelectedItem}
                onCancelClick={this.undoConfirmDelete}
              >
                <div className="mbxl">
                  <p className="type-bold">Are you sure you want to unschedule this item?</p>

                  {this.getSelectedItem() ? (
                    <p className="type-s">
                      <span><ScheduledItemTitle scheduledAction={this.getSelectedItem()} behaviorGroups={this.props.behaviorGroups}/> </span>
                      <span>{this.getSelectedItem().recurrence.displayString}</span>
                    </p>
                  ) : null}
                </div>
              </ConfirmActionPanel>
            </Collapsible>
            <Collapsible revealWhen={this.state.justSaved}>
              <div className="container pvxl border-top">
                <span className="fade-in type-green type-bold type-italic">Your changes have been saved.</span>
              </div>
            </Collapsible>
            <Collapsible revealWhen={this.state.justDeleted}>
              <div className="container pvxl border-top">
                <span className="fade-in type-green type-bold type-italic">The item has been unscheduled.</span>
              </div>
            </Collapsible>
            <Collapsible revealWhen={this.shouldShowActions()}>
              <div className="container ptl pbs border-top">
                <DynamicLabelButton
                  disabledWhen={!this.selectedItemHasChanges() || this.hasActiveRequest() || !selectedItemIsValid}
                  className="button-primary mbs mrs"
                  onClick={this.saveSelectedItem}
                  labels={[
                    { text: "Save schedule", displayWhen: selectedItemIsNew && !this.props.isSaving },
                    { text: "Save changes", displayWhen: !selectedItemIsNew && !this.props.isSaving },
                    { text: "Saving…", displayWhen: this.props.isSaving }
                  ]}
                />
                <DynamicLabelButton
                  disabledWhen={this.hasActiveRequest()}
                  className="mbs mrs"
                  onClick={this.cancelEditor}
                  labels={[
                    { text: "Cancel", displayWhen: selectedItemIsNew },
                    { text: "Undo changes", displayWhen: !selectedItemIsNew && this.selectedItemHasChanges() },
                    { text: "Done", displayWhen: !selectedItemIsNew && !this.selectedItemHasChanges() }
                  ]}
                />
                {selectedItemIsNew ? null : (
                  <button
                    type="button"
                    className="mrs mbs"
                    onClick={this.confirmDeleteItem}
                    disabled={this.hasActiveRequest()}
                  >Unschedule this</button>
                )}
                {this.props.error ? (
                  <span className="fade-in">
                    <span className="align-button mbs mrm" />
                    <span className="align-button mbs type-pink type-bold type-italic"> {this.props.error}</span>
                  </span>
                ) : null}
              </div>
            </Collapsible>
          </FixedFooter>
        </div>
      );
    }
  });

  return PageWithPanels.with(Scheduling);
});
