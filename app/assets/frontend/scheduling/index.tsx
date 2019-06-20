import * as React from 'react';
import BrowserUtils from '../lib/browser_utils';
import Button from '../form/button';
import ChannelName from './channel_name';
import Collapsible from '../shared_ui/collapsible';
import ConfirmActionPanel from '../panels/confirm_action';
import DynamicLabelButton from '../form/dynamic_label_button';
import BehaviorGroup from '../models/behavior_group';
import ScheduledAction from '../models/scheduled_action';
import ScheduleChannel from '../models/schedule_channel';
import ScheduledItem from './scheduled_item';
import ScheduledItemEditor from './scheduled_item_editor';
import ScheduledItemTitle from './scheduled_item_title';
import Sort from '../lib/sort';
import {PageRequiredProps} from '../shared_ui/page';
import autobind from '../lib/autobind';
import {UserMap, ValidTriggerInterface} from "./data_layer";
import User from '../models/user';
import OrgChannels from "../models/org_channels";
import {DataRequest} from "../lib/data_request";
import {TimeZoneData} from "../time_zone/team_time_zone_setter";
import Select from "../form/select";

export interface SchedulingProps {
  groupId: Option<string>,
  sidebarWidth: number,
  scheduledActions: Array<ScheduledAction>,
  orgChannels: OrgChannels,
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
  filterChannelId: Option<string>,
  filterBehaviorGroupId: Option<string>,
  newAction: Option<boolean>,
  isAdmin: boolean,
  userMap: UserMap,
  onLoadUserData: (userId: string) => void,
  csrfToken: string,
  validTriggers: Array<ValidTriggerInterface>,
  isValidatingTriggers: boolean,
  triggerValidationError: Option<string>
}

type Props = SchedulingProps & PageRequiredProps

type State = {
  filterBehaviorGroupId: string,
  filterChannelId: string,
  selectedItem: Option<ScheduledAction>,
  justSaved: boolean,
  justDeleted: boolean,
  isEditing: boolean,
  userTimeZone: string | null,
  userTimeZoneName: string | null,
  headerHeight: number
}

type ScheduleGroup = {
  channel: Option<ScheduleChannel>,
  channelName: Option<string>,
  channelId: string,
  excludesBot: boolean,
  isArchived: boolean,
  isMissing: boolean,
  isReadOnly: boolean,
  actions: Array<ScheduledAction>
}

type SchedulesGroupedByName = {
  [groupName: string]: ScheduleGroup | undefined
}

class Scheduling extends React.Component<Props, State> {
    header: Option<HTMLDivElement>;

    constructor(props: Props) {
      super(props);
      autobind(this);
      const selectedItem = this.getDefaultSelectedItem();
      this.state = {
        filterChannelId: this.props.filterChannelId || "",
        filterBehaviorGroupId: this.props.filterBehaviorGroupId || "",
        selectedItem: selectedItem,
        justSaved: false,
        justDeleted: false,
        isEditing: Boolean(selectedItem),
        userTimeZone: null,
        userTimeZoneName: null,
        headerHeight: 0
      };
    }

    componentWillReceiveProps(nextProps: Props): void {
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
          filterChannelId: hasRemainingActionsInFilteredChannel ? this.state.filterChannelId : "",
          selectedItem: nextProps.error ? this.state.selectedItem : null,
          justSaved: false,
          justDeleted: !nextProps.error,
          isEditing: Boolean(nextProps.error)
        });
      }
    }

    componentDidMount(): void {
      this.updateURL();
      this.getUserTimeZoneName();
      this.resetHeaderHeight();
    }

    componentWillUpdate(nextProps: Props, nextState: State, nextContext: any): void {
      const nextBehaviorGroupId = nextState.filterBehaviorGroupId;
      if (nextBehaviorGroupId && nextBehaviorGroupId !== this.state.filterBehaviorGroupId && nextState.filterChannelId) {
        const matchingActions = this.props.scheduledActions.filter((action) => action.channel === nextState.filterChannelId &&
          this.scheduledActionTriggersBehaviorGroupId(action, nextBehaviorGroupId));
        if (!matchingActions.length && this.props.scheduledActions.length > 0) {
          this.setState({
            filterChannelId: ""
          });
        }
      }
    }

    componentDidUpdate(prevProps: Props, prevState: State): void {
      if (prevState.isEditing !== this.state.isEditing) {
        window.scrollTo(0, 0);
      }
      this.updateURL();
      this.resetHeaderHeight();
    }

    resetHeaderHeight(): void {
      const newHeight = this.header ? this.header.offsetHeight : 0;
      if (this.state.headerHeight !== newHeight) {
        this.setState({
          headerHeight: newHeight
        });
      }
    }

    isForSingleGroup(): boolean {
      return Boolean(this.props.groupId);
    }

    updateURL(): void {
      const explicitTeamId = BrowserUtils.hasQueryParam("teamId") ? this.props.teamId : null;
      const forceAdmin = BrowserUtils.hasQueryParamWithValue("forceAdmin", true) || null;
      BrowserUtils.replaceURL(this.getCorrectedURL(explicitTeamId, forceAdmin));
    }

    getUserTimeZoneName(): void {
      const timeZoneId = Intl.DateTimeFormat().resolvedOptions().timeZone;
      if (timeZoneId) {
        const url = jsRoutes.controllers.ApplicationController.getTimeZoneInfo(timeZoneId).url;
        DataRequest.jsonGet(url).then((tzInfo: TimeZoneData) => {
          if (tzInfo.formattedName) {
            this.setState({
              userTimeZone: tzInfo.tzName,
              userTimeZoneName: tzInfo.formattedName
            });
          }
        }).catch(() => {});
      }
    }

    renderNavItems() {
      const items = [];
      if (this.state.isEditing) {
        const item = this.getSelectedItem();
        items.push({
          title: item && !item.isNew() ? "Edit schedule" : "New schedule"
        });
      }
      return this.props.onRenderNavItems(items);
    }

    renderNavActions() {
      const actions = this.state.isEditing ? null : (
        <div className="fade-in height-xl mvl">
          <Button className="button-shrink button-s" onClick={this.addNewItem}>Schedule something new</Button>
        </div>
      );
      return this.props.onRenderNavActions(actions);
    }

    hasChannelList(): boolean {
      return this.props.orgChannels.allChannels().length > 0;
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

    prepareRoute(selectedItemId: Option<string>, isNewSchedule: Option<boolean>, filterChannelId: Option<string>, filterBehaviorGroupId: Option<string>, explicitTeamId: Option<string>, forceAdmin: Option<boolean>): JsRoute {
      if (this.props.groupId) {
        return jsRoutes.controllers.BehaviorGroupConfigController.schedules(this.props.groupId);
      } else {
        return jsRoutes.controllers.ScheduledActionsController.index(selectedItemId, isNewSchedule, filterChannelId, filterBehaviorGroupId, explicitTeamId, forceAdmin);
      }
    }

    getCorrectedURL(explicitTeamId: Option<string>, forceAdmin: Option<boolean>): string {
      const filterChannelId = this.state.filterChannelId || null;
      const filterBehaviorGroupId = this.state.filterBehaviorGroupId || null;
      if (this.state.isEditing && this.state.selectedItem && !this.state.selectedItem.isNew()) {
        return this.prepareRoute(this.state.selectedItem.id, null, filterChannelId, filterBehaviorGroupId, explicitTeamId, forceAdmin).url;
      } else if (this.state.isEditing && this.state.selectedItem) {
        return this.prepareRoute(null, true, filterChannelId, filterBehaviorGroupId, explicitTeamId, forceAdmin).url;
      } else {
        return this.prepareRoute(null, null, filterChannelId, filterBehaviorGroupId, explicitTeamId, forceAdmin).url;
      }
    }

    isEditing(): boolean {
      return this.state.isEditing;
    }

    getSelectedItem(): Option<ScheduledAction> {
      return this.state.selectedItem;
    }

    updateSelectedItem(newItem: ScheduledAction, optionalCallback?: () => void): void {
      this.setState({
        selectedItem: newItem
      }, optionalCallback);
    }

    findChannelFor(channelId: string): Option<ScheduleChannel> {
      return this.hasChannelList() && this.props.orgChannels.allChannels().find((ea) => ea.id === channelId) || null;
    }

    groupForChannel(channel: Option<ScheduleChannel>, channelName: Option<string>): ScheduleGroup {
      return {
        channel: channel,
        channelName: channelName,
        channelId: channel ? channel.id : "unknown",
        excludesBot: Boolean(channel && !channel.isDm() && !channel.isBotMember),
        isArchived: Boolean(channel && channel.isArchived),
        isMissing: !channel,
        isReadOnly: Boolean(channel && channel.isReadOnly),
        actions: []
      };
    }

    scheduledActionTriggersBehaviorGroupId(action: ScheduledAction, behaviorGroupId: string): boolean {
      if (action.behaviorGroupId) {
        return action.behaviorGroupId === behaviorGroupId;
      } else {
        const matchingTriggers = this.props.validTriggers.filter((ea) => ea.text === action.trigger);
        const behaviorGroup = this.props.behaviorGroups.find((ea) => ea.id === behaviorGroupId);
        return matchingTriggers.some((trigger) => {
          return trigger.matchingBehaviorTriggers.some((matchingBehavior) => {
            return Boolean(behaviorGroup && behaviorGroup.behaviorVersions.some((behaviorVersion) => behaviorVersion.behaviorId === matchingBehavior.behaviorId));
          });
        });
      }
    }

    getScheduleByChannel(): Array<ScheduleGroup> {
      const groupsByName: SchedulesGroupedByName = {};
      this.props.scheduledActions.forEach((action) => {
        const filterSkillId = this.state.filterBehaviorGroupId;
        const includeAction = !filterSkillId || this.scheduledActionTriggersBehaviorGroupId(action, filterSkillId);
        if (includeAction) {
          const channel = this.findChannelFor(action.channel);
          const channelName = channel ? channel.getFormattedName() : null;
          const groupName = channelName || "[unknown]";
          const group = groupsByName[groupName] || this.groupForChannel(channel, channelName);
          group.actions.push(action);
          groupsByName[groupName] = group;
        }
      });
      if (this.props.filterChannelId && !this.props.scheduledActions.some((ea) => ea.channel === this.props.filterChannelId)) {
        const channel = this.findChannelFor(this.props.filterChannelId);
        const channelName = channel ? channel.getFormattedName() : null;
        const groupName = channelName || "[unknown]";
        if (!groupsByName[groupName]) {
          groupsByName[groupName] = this.groupForChannel(channel, channelName);
        }
      }
      const groupArray = Object.keys(groupsByName).map((channelName) => {
        const group = groupsByName[channelName] as ScheduleGroup;
        group.actions = Sort.arrayAscending(group.actions, (action) => action.firstRecurrence ? Number(action.firstRecurrence) : Infinity);
        return group;
      });
      return Sort.arrayAscending(groupArray, (group) => {
        const firstAction = group.actions[0];
        return firstAction && firstAction.firstRecurrence ? Number(firstAction.firstRecurrence) : Infinity
      });
    }

    shouldShowChannel(channelId: string): boolean {
      return !this.state.filterChannelId || this.state.filterChannelId === channelId;
    }

    changeChannelFilter(channelId: string) {
      this.setState({
        filterChannelId: this.filterActiveFor(channelId) ? "" : channelId
      });
    }

    filterActiveFor(channelId: string): boolean {
      return this.state.filterChannelId === channelId;
    }

    toggleEditor(action: ScheduledAction) {
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

    lookupUser(userId: string): Option<User> {
      const user = this.props.userMap[userId];
      if (!user) {
        this.props.onLoadUserData(userId);
      }
      return user;
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

    getTriggerValidationFor(action: ScheduledAction) {
      return this.props.validTriggers.find((ea) => ea.text === action.trigger);
    }

    renderSubheading() {
      if (!this.hasChannelList()) {
        return (
          <div className="ptxl">
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

    updateSkillFilter(skillId: string): void {
      this.setState({
        filterBehaviorGroupId: skillId
      });
    }

    getSkillOptions(): Array<{ id: string, name: string}> {
      return this.props.behaviorGroups.map((group) => ({
        id: group.id || "",
        name: group.getName()
      })).filter((ea) => Boolean(ea.id));
    }

    renderGroupWarningText(group: ScheduleGroup) {
      if (group.isArchived) {
        return (
          <span className="type-s type-pink">
            Warning: This channel is archived.
          </span>
        );
      } else if (group.excludesBot) {
        return (
          <span className="type-s type-pink">
            Warning: The bot must be invited to this channel for any scheduled action to run.
          </span>
        );
      } else if (group.isMissing) {
        return (
          <span className="type-s type-pink">
            Warning: No information about this channel was found. It may be private and the bot is not a member, or it may no longer exist.
          </span>
        );
      } else if (group.isReadOnly) {
        return (
          <span className="type-s type-pink">
            Warning: The bot is restricted from posting to this channel by the admin.
          </span>
        );
      } else {
        return null;
      }
    }

    renderGroups(groups: Array<ScheduleGroup>) {
      return groups.map((group, index) => {
        const hasActions = group.actions.length > 0;
        return (
          <Collapsible key={`group-${group.channelId || "unknown"}`} revealWhen={this.shouldShowChannel(group.channelId)}>
            <div className={`columns pvl ${index > 0 ? "border-top border-light" : ""}`}>
              <div className={`column ${
                this.isForSingleGroup() ? "column-full" : "column-page-sidebar ptm"
              } mobile-ptn prn`}>
                <div className="container">
                  <h4 className="mvn"><ChannelName channel={group.channel} /></h4>
                  <div>{this.renderGroupWarningText(group)}</div>
                </div>
              </div>

              <div className={`column ${
                this.isForSingleGroup() ? "column-full" : "column-page-main"
              } pln`}>
                <div className="container container-narrow">
                  {hasActions ? group.actions.map((action) => (
                    <ScheduledItem
                      key={`${action.scheduleType}-${action.id}`}
                      className={`mvs pal mobile-pam border border-light bg-white`}
                      scheduledAction={action}
                      behaviorGroups={this.props.behaviorGroups}
                      onClick={this.toggleEditor}
                      userTimeZone={this.state.userTimeZone}
                      userTimeZoneName={this.state.userTimeZoneName}
                      validTrigger={this.getTriggerValidationFor(action)}
                    />
                  )) : (
                    <div className="mhl mvs pal mobile-pam border border-light bg-white">
                      There are no scheduled actions in this channel.
                    </div>
                  )}
                </div>
              </div>
            </div>
          </Collapsible>
        );
      });
    }

    renderNoSchedules() {
      return (
        <div className={"container container-wide pvxl"}>
          {this.hasChannelList() ? this.renderNoSchedulesMessage() : this.renderErrorMessage()}
        </div>
      );
    }

    renderNoSchedulesMessage() {
      const selectedGroup = this.state.filterBehaviorGroupId ? this.props.behaviorGroups.find((ea) => ea.id === this.state.filterBehaviorGroupId) : null;
      return (
        <div>
          <p className="type-bold">{this.renderNoScheduleMessageFor(selectedGroup)}</p>

          <p>You can schedule any action to run on a recurring basis in a particular channel.</p>

        </div>
      );
    }

    renderNoScheduleMessageFor(selectedGroup: Option<BehaviorGroup>) {
      if (selectedGroup && this.props.isValidatingTriggers) {
        return (
          <span className="pulse">Checking scheduled messages…</span>
        );
      } else if (selectedGroup && !this.props.isValidatingTriggers) {
        return (
          <span>Nothing is currently scheduled in the <b>{selectedGroup.getName()}</b> skill.</span>
        );
      } else {
        return (
          <span>Nothing is currently scheduled on this team.</span>
        );
      }

    }

    renderErrorMessage() {
      return (
        <div>
          <p className="type-bold">No scheduling information was found.</p>

          <p>There may be an error, or you may not have access to this information.</p>
        </div>
      );
    }

    renderScheduleList(groups: Array<ScheduleGroup>) {
      return groups.length > 0 ? this.renderGroups(groups) : this.renderNoSchedules();
    }

    renderHeader(groups: Array<ScheduleGroup>) {
      return (
        <div className={this.props.isMobile ? "" : "position-fixed"}
          ref={(el) => this.header = el}
          style={{
            top: this.props.headerHeight,
            left: this.props.sidebarWidth - (this.isForSingleGroup() ? 1 : 0),
            right: 0
          }}
        >
          <Collapsible revealWhen={!this.isEditing()} onChange={this.resetHeaderHeight}>
            <div className={`bg-white-translucent border-bottom border-light ${this.isForSingleGroup() ? "border-left" : ""}`}>
              <div className="container container-narrow pts">
                {this.isForSingleGroup() ? null : (
                  <div className="display-inline-block align-m mrl mbs">
                    <h5 className="display-inline-block mtn mrs">Skill</h5>
                    <Select value={this.state.filterBehaviorGroupId || ""} className="form-select-s align-m" onChange={this.updateSkillFilter}>
                      <option value="">All skills</option>
                      {this.getSkillOptions().map((ea) => (
                        <option key={`filterSkillId-${ea.id}`} value={ea.id}>{ea.name}</option>
                      ))}
                    </Select>
                  </div>
                )}
                <div className="display-inline-block align-m mbs">
                  <h5 className="display-inline-block mtn mrs">Channel</h5>
                  <Select value={this.state.filterChannelId} className="form-select-s align-m" onChange={this.changeChannelFilter}>
                    <option value="">All channels</option>
                    {groups.map((group) => (
                      <option key={`filterChannelId-${group.channelId}`} value={group.channelId}>{group.channelName || "Unknown channel"}</option>
                    ))}
                  </Select>
                </div>
              </div>
            </div>
          </Collapsible>
        </div>
      );
    }

    render() {
      const groups = this.getScheduleByChannel();
      const selectedItem = this.getSelectedItem();
      const selectedItemChannel = selectedItem ? this.findChannelFor(selectedItem.channel) : null;
      const selectedItemIsValid = Boolean(selectedItem && selectedItem.isValid());
      const selectedItemIsNew = Boolean(selectedItem && selectedItem.isNew());
      return (
        <div className="flex-row-cascade" style={{
          paddingTop: this.props.isMobile ? 0 : this.state.headerHeight,
          paddingBottom: this.props.footerHeight
        }}>
          {this.renderHeader(groups)}
          <Collapsible revealWhen={!this.isEditing()} className={"flex-row-cascade mobile-flex-no-expand"}>
            <div>
              {this.renderSubheading()}
              {this.renderScheduleList(groups)}
            </div>
          </Collapsible>
          <Collapsible revealWhen={this.isEditing()}>
            {this.isEditing() ? (
              <ScheduledItemEditor
                isForSingleGroup={this.isForSingleGroup()}
                teamId={this.props.teamId}
                scheduledAction={selectedItem}
                orgChannels={this.props.orgChannels}
                behaviorGroups={this.props.behaviorGroups}
                onChange={this.updateSelectedItem}
                teamTimeZone={this.props.teamTimeZone || "America/New_York"}
                teamTimeZoneName={this.props.teamTimeZoneName || "Eastern Time"}
                slackUserId={this.props.slackUserId || ""}
                slackBotUserId={this.props.slackBotUserId || ""}
                isAdmin={this.props.isAdmin}
                scheduleUser={selectedItem && selectedItem.userId ? this.lookupUser(selectedItem.userId) : null}
                userTimeZoneName={this.state.userTimeZoneName}
                csrfToken={this.props.csrfToken}
              />
            ) : null}
          </Collapsible>

          {this.props.onRenderFooter((
            <div>
            <Collapsible
              revealWhen={this.props.activePanelName === 'confirmDelete'}
              ref={(el) => this.props.onRenderPanel("confirmDelete", el)}
              onChange={this.props.onRevealedPanel}
            >
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
                                `In ${selectedItemChannel.getDescription()}` :
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
              <div className="container container-wide pvxl border-top">
                <span className="fade-in type-green type-bold type-italic">Your changes have been saved.</span>
              </div>
            </Collapsible>
            <Collapsible revealWhen={this.state.justDeleted}>
              <div className="container container-wide pvxl border-top">
                <span className="fade-in type-green type-bold type-italic">The item has been unscheduled.</span>
              </div>
            </Collapsible>
            <Collapsible revealWhen={!this.state.justSaved && !this.state.justDeleted && Boolean(this.props.triggerValidationError)}>
              <div className="container container-wide pvxl border-top">
                <span className="fade-in type-pink type-bold type-italic">{this.props.triggerValidationError}</span>
              </div>
            </Collapsible>
            <Collapsible revealWhen={this.shouldShowActions()}>
              <div className="container container-wide ptl pbs border-top">
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
          ), null, {
            marginLeft: this.props.sidebarWidth
          })}
          {this.renderNavItems()}
          {this.renderNavActions()}
        </div>
      );
    }
}

export default Scheduling;
