import * as React from 'react';
import BrowserUtils from '../lib/browser_utils';
import Button from '../form/button';
import ChannelName from './channel_name';
import Collapsible from '../shared_ui/collapsible';
import ConfirmActionPanel from '../panels/confirm_action';
import DynamicLabelButton from '../form/dynamic_label_button';
import Page from '../shared_ui/page';
import BehaviorGroup from '../models/behavior_group';
import ScheduledAction from '../models/scheduled_action';
import ScheduleChannel from '../models/schedule_channel';
import ScheduledItem from './scheduled_item';
import ScheduledItemEditor from './scheduled_item_editor';
import ScheduledItemTitle from './scheduled_item_title';
import Sort from '../lib/sort';
import {PageRequiredProps} from '../shared_ui/page';
import autobind from '../lib/autobind';

export interface SchedulingProps {
  scheduledActions: Array<ScheduledAction>
  channelList: Array<ScheduleChannel>,
  behaviorGroups: Array<BehaviorGroup>,
  teamId: string,
  teamTimeZone: Option<string>,
  teamTimeZoneName: Option<string>,
  slackUserId: string,
  slackBotUserId: string,
  onSave: (scheduled: ScheduledAction) => void,
  isSaving: boolean,
  onDelete: (scheduled: ScheduledAction) => void,
  isDeleting: boolean,
  error: Option<string>,
  onClearErrors: () => void,
  justSavedAction: Option<ScheduledAction>,
  selectedScheduleId: Option<string>,
  newAction: Option<boolean>
}

type Props = SchedulingProps & PageRequiredProps

type State = {
  filterChannelId: Option<string>,
  selectedItem: Option<ScheduledAction>,
  justSaved: boolean,
  justDeleted: boolean,
  isEditing: boolean
}

type ScheduleGroup = {
  channel: Option<ScheduleChannel>,
  channelName: string,
  channelId: string,
  excludesBot: boolean,
  excludesUser: boolean,
  actions: Array<ScheduledAction>
}

class Scheduling extends React.Component<Props, State> {

    static defaultProps: PageRequiredProps;

    constructor(props) {
      super(props);
      autobind(this);
      const selectedItem = this.getDefaultSelectedItem();
      this.state = {
        filterChannelId: selectedItem && this.hasChannelList() ? selectedItem.channel : null,
        selectedItem: selectedItem,
        justSaved: false,
        justDeleted: false,
        isEditing: Boolean(selectedItem)
      };
    }

    componentWillReceiveProps(nextProps) {
      const justSaved = this.props.isSaving && !nextProps.isSaving;
      const justDeleted = this.props.isDeleting && !nextProps.isDeleting;
      const newAction = justSaved ? nextProps.justSavedAction : null;
      if (justSaved || justDeleted) {
        this.props.onClearActivePanel();
      }
      if (justSaved) {
        this.setState({
          selectedItem: newAction || this.state.selectedItem,
          justSaved: !nextProps.error,
          justDeleted: false,
          isEditing: Boolean(nextProps.error)
        });
      } else if (justDeleted) {
        const hasRemainingActionsInFilteredChannel = this.state.filterChannelId ?
          nextProps.scheduledActions.some((ea) => ea.channel === this.state.filterChannelId) : false;
        this.setState({
          filterChannelId: hasRemainingActionsInFilteredChannel ? this.state.filterChannelId : null,
          selectedItem: nextProps.error ? this.state.selectedItem : null,
          justSaved: false,
          justDeleted: !nextProps.error,
          isEditing: Boolean(nextProps.error)
        });
      }
    }

    componentDidMount() {
      this.renderNavItems();
      this.renderNavActions();
    }

    componentDidUpdate(prevProps, prevState) {
      if (prevState.isEditing !== this.state.isEditing) {
        window.scrollTo(0, 0);

        const explicitTeamId = BrowserUtils.hasQueryParam("teamId") ? this.props.teamId : null;
        const forceAdmin = BrowserUtils.hasQueryParamWithValue("forceAdmin", true);
        BrowserUtils.replaceURL(this.getCorrectedURL(explicitTeamId, forceAdmin);
      }
      this.renderNavItems();
      this.renderNavActions();
    }

    renderNavItems() {
      const items = [{
        title: "Scheduling"
      }];
      if (this.state.isEditing) {
        const item = this.getSelectedItem();
        items.push({
          title: item && !item.isNew() ? "Edit schedule" : "New schedule"
        });
      }
      this.props.onRenderNavItems(items);
    }

    renderNavActions() {
      const actions = this.state.isEditing ? null : (
        <div className="fade-in height-xl mvl">
          <Button className="button-shrink button-s" onClick={this.addNewItem}>Schedule something new</Button>
        </div>
      );
      this.props.onRenderNavActions(actions);
    }

    hasChannelList(): boolean {
      return Boolean(this.props.channelList) && this.props.channelList.length > 0;
    }

    getDefaultSelectedItem(): Option<ScheduledAction> {
      if (this.props.selectedScheduleId) {
        return this.props.scheduledActions.find((ea) => ea.id === this.props.selectedScheduleId) || null;
      } else if (this.props.newAction) {
        return this.createNewSchedule();
      } else {
        return null;
      }
    }

    getCorrectedURL(explicitTeamId, forceAdmin): string {
      if (this.state.isEditing && this.state.selectedItem && !this.state.selectedItem.isNew()) {
        return jsRoutes.controllers.ScheduledActionsController.index(this.state.selectedItem.id, null, explicitTeamId, forceAdmin).url;
      } else if (this.state.isEditing && this.state.selectedItem) {
        return jsRoutes.controllers.ScheduledActionsController.index(null, true, explicitTeamId, forceAdmin).url;
      } else {
        return jsRoutes.controllers.ScheduledActionsController.index(null, null, explicitTeamId, forceAdmin).url;
      }
    }

    isEditing(): boolean {
      return this.state.isEditing;
    }

    getSelectedItem(): Option<ScheduledAction> {
      return this.state.selectedItem;
    }

    updateSelectedItem(newItem, optionalCallback) {
      this.setState({
        selectedItem: newItem
      }, optionalCallback);
    }

    findChannelFor(channelId): Option<ScheduleChannel> {
      return this.hasChannelList() && this.props.channelList.find((ea) => ea.id === channelId) || null;
    }

    getScheduleByChannel(): Array<ScheduleGroup> {
      const groupsByName = {};
      this.props.scheduledActions.forEach((action) => {
        const channel = this.findChannelFor(action.channel);
        const channelName = channel ? channel.getFormattedName(this.props.slackUserId) : "Unknown";
        const excludesUser = channel ? !channel.userCanAccess(this.props.slackUserId) : false;
        const excludesBot = this.props.slackBotUserId && channel ? !channel.isDM() && !channel.userCanAccess(this.props.slackBotUserId) : false;
        if (!channel || channel.isPublic || !excludesUser || channel.isDM()) {
          const group = groupsByName[channelName] || {
            channel: channel,
            channelName: channelName,
            channelId: channel ? channel.id : "unknown",
            excludesBot: excludesBot,
            excludesUser: excludesUser,
            actions: []
          };
          group.actions.push(action);
          groupsByName[channelName] = group;
        }
      });
      const channelNames = Object.keys(groupsByName);
      const sortedNames = Sort.arrayAlphabeticalBy(channelNames, (ea) => ea);
      return sortedNames.map((channelName) => groupsByName[channelName]);
    }

    shouldShowChannel(channelId): boolean {
      return !this.state.filterChannelId || this.state.filterChannelId === channelId;
    }

    toggleFilter(channelId) {
      this.setState({
        filterChannelId: this.filterActiveFor(channelId) ? null : channelId
      });
    }

    clearFilters() {
      this.setState({
        filterChannelId: null
      });
    }

    filterActiveFor(channelId): boolean {
      return this.state.filterChannelId === channelId;
    }

    toggleEditor(action) {
      this.props.onClearErrors();
      this.setState({
        selectedItem: action,
        justSaved: false,
        justDeleted: false,
        isEditing: true
      });
    }

    createNewSchedule(): ScheduledAction {
      return ScheduledAction.newWithDefaults(this.props.teamTimeZone, this.props.teamTimeZoneName);
    }

    addNewItem() {
      this.props.onClearErrors();
      this.setState({
        selectedItem: this.createNewSchedule(),
        justSaved: false,
        justDeleted: false,
        isEditing: true
      });
    }

    cancelEditor() {
      this.setState({
        selectedItem: null,
        isEditing: false
      });
    }

    saveSelectedItem() {
      const item = this.getSelectedItem();
      if (item) {
        this.props.onSave(item);
      }
    }

    confirmDeleteItem() {
      this.props.onToggleActivePanel("confirmDelete", true);
    }

    deleteSelectedItem() {
      const item = this.getSelectedItem();
      if (item) {
        this.props.onDelete(item);
      }
    }

    undoConfirmDelete() {
      this.props.onClearActivePanel();
    }

    selectedItemHasChanges(): boolean {
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
    }

    renderSubheading() {
      if (!this.hasChannelList()) {
        return (
          <div className="ptxl container container-wide">
            <div className="type-pink type-italic type-m align-m">
              Warning: An error occurred loading channel information
            </div>
          </div>
        );
      } else {
        return null;
      }
    }

    shouldShowActions(): boolean {
      return this.isEditing() && !this.props.activePanelName;
    }

    hasActiveRequest(): boolean {
      return this.props.isSaving || this.props.isDeleting;
    }

    renderSidebar(groups) {
      return (
        <div>
          <div className="phxl mobile-phl mbs">
            <h5 className="mtn display-inline-block prm">Channel</h5>
          </div>

          <div>
            <Button
              className={`button-block width-full phxl mobile-phl pvxs mvxs ${
                this.state.filterChannelId ? "type-link" : "bg-blue type-white"
              }`}
              onClick={this.clearFilters}
            >
              <span className={"type-bold"}>All channels</span>
            </Button>
            {this.renderFilterList(groups)}
          </div>
        </div>
      );
    }

    renderFilterList(groups) {
      if (groups.length > 1) {
        return groups.map((group) => (
          <Button
            className={`button-block width-full phxl mobile-phl pvxs mvxs ${
              this.filterActiveFor(group.channelId) ? "bg-blue type-white " : "type-link "
              }`}
            key={`filter-${group.channelName}`}
            onClick={() => this.toggleFilter(group.channelId)}
          >
            {group.channelName}
          </Button>
        ));
      }
    }

    renderGroupWarning(group) {
      if (group.excludesBot) {
        return (
          <span className="type-s type-pink type-bold type-italic">
            — Warning: Ellipsis must be invited to this channel for any scheduled action to run.
          </span>
        );
      } else if (group.excludesUser) {
        return (
          <span className="type-s type-weak type-italic">
            — You are not a member of this channel.
          </span>
        );
      } else {
        return null;
      }
    }

    renderGroups(groups) {
      return groups.map((group) => (
        <Collapsible key={`group-${group.channelId || "unknown"}`} revealWhen={this.shouldShowChannel(group.channelId)}>
          <div className="ptxl pbxl">
            <div className="phxl mobile-phl">
              <h4 className="mvn">
                <span className="mrxs"><ChannelName channel={group.channel} slackUserId={this.props.slackUserId} /></span>
                {this.renderGroupWarning(group)}
              </h4>
            </div>

            <div>
              {group.actions.map((action) => (
                <ScheduledItem
                  key={`${action.type}-${action.id}`}
                  className={`mhl mvs pal mobile-pam border border-light bg-white`}
                  scheduledAction={action}
                  behaviorGroups={this.props.behaviorGroups}
                  onClick={this.toggleEditor}
                />
              ))}
            </div>
          </div>
        </Collapsible>
      ));
    }

    renderNoSchedules() {
      return (
        <div className={"container container-wide pvxl"}>
          {this.hasChannelList() ? this.renderNoSchedulesMessage() : this.renderErrorMessage()}
        </div>
      );
    }

    renderNoSchedulesMessage() {
      return (
        <div>
          <p className="type-bold">Nothing is currently scheduled in channels you can access on this team.</p>

          <p>You can schedule any action to run on a recurring basis in a particular channel.</p>

        </div>
      );
    }

    renderErrorMessage() {
      return (
        <div>
          <p className="type-bold">No scheduling information was found.</p>

          <p>There may be an error, or you may not have access to this information.</p>
        </div>
      );
    }

    renderScheduleList(groups) {
      return groups.length > 0 ? this.renderGroups(groups) : this.renderNoSchedules();
    }

    render() {
      const groups = this.getScheduleByChannel();
      const selectedItem = this.getSelectedItem();
      const selectedItemChannel = selectedItem ? this.findChannelFor(selectedItem.channel) : null;
      const selectedItemIsValid = Boolean(selectedItem && selectedItem.isValid());
      const selectedItemIsNew = Boolean(selectedItem && selectedItem.isNew());
      return (
        <div className="flex-row-cascade">
          <Collapsible revealWhen={!this.isEditing()} className={"flex-row-cascade mobile-flex-no-expand"}>
            <div className="flex-columns flex-row-expand">
              <div className="flex-column flex-column-left flex-rows container container-wide phn">
                <div className="columns flex-columns flex-row-expand mobile-flex-no-columns">
                  <div className="column column-one-quarter flex-column mobile-column-full ptxl phn bg-white border-right border-light">
                    {this.renderSidebar(groups)}
                  </div>
                  <div className="column mobile-column-full pbxxxxl column-three-quarters flex-column">
                    {this.renderSubheading()}
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

          {this.props.onRenderFooter((
            <div>
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

                  {selectedItem ? (
                    <div className="columns columns-elastic type-s">
                      <div className="column-group">
                        <div className="column-row">
                          <div className="column column-shrink">
                            <h6 className="mtxs">What</h6>
                          </div>
                          <div className="column column-expand">
                            <p>
                              <ScheduledItemTitle scheduledAction={selectedItem} behaviorGroups={this.props.behaviorGroups}/>
                            </p>
                          </div>
                        </div>
                        <div className="column-row">
                          <div className="column column-shrink">
                            <h6 className="mtxs">Where</h6>
                          </div>
                          <div className="column column-expand">
                            <p>
                              {selectedItemChannel ?
                                `In ${selectedItemChannel.getDescription(this.props.slackUserId)}` :
                                `In channel ID ${selectedItem.channel}`
                              }
                            </p>
                          </div>
                        </div>
                        <div className="column-row">
                          <div className="column column-shrink">
                            <h6 className="mtxs">When</h6>
                          </div>
                          <div className="column column-expand">
                            <p>{selectedItem.recurrence.displayString}</p>
                          </div>
                        </div>
                      </div>
                    </div>
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
            </div>
          ))}
        </div>
      );
    }
}

Scheduling.defaultProps = Page.requiredPropDefaults();

export default Scheduling;
