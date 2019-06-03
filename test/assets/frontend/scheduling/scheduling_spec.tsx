import * as React from 'react';
import * as TestUtils from 'react-dom/test-utils';
import * as MockDataRequest from '../../../mocks/mock_data_request';
import Scheduling from '../../../../app/assets/frontend/scheduling/index';
import Recurrence from '../../../../app/assets/frontend/models/recurrence';
import ScheduledAction, {ScheduledActionInterface} from '../../../../app/assets/frontend/models/scheduled_action';
import ScheduleChannel, {ScheduleChannelInterface} from '../../../../app/assets/frontend/models/schedule_channel';
import ID from '../../../../app/assets/frontend/lib/id';
import {SchedulingProps} from "../../../../app/assets/frontend/scheduling";
import OrgChannels from "../../../../app/assets/frontend/models/org_channels";
import TeamChannels from "../../../../app/assets/frontend/models/team_channels";
import {getPageRequiredProps} from "../../../mocks/mock_page";
import BehaviorGroup from "../../../../app/assets/frontend/models/behavior_group";
import BehaviorVersion from "../../../../app/assets/frontend/models/behavior_version";
import BehaviorConfig from "../../../../app/assets/frontend/models/behavior_config";

jest.mock('../../../../app/assets/frontend/lib/data_request', () => MockDataRequest);
jest.mock('../../../../app/assets/frontend/lib/browser_utils');

jsRoutes.controllers.ScheduledActionsController.index = () => ({ url: "/test", method: "get", absoluteURL: () => "https://nope/" });
jsRoutes.controllers.ApplicationController.getTimeZoneInfo = () => ({ url: "/nope", method: "get", absoluteURL: () => "https://nopenope/" });

Object.defineProperty(window, "scrollTo", {
  value: jest.fn()
});

class Loader extends React.Component<SchedulingProps, SchedulingProps> {
  page: Scheduling;
  constructor(props: SchedulingProps) {
    super(props);
    this.state = Object.assign({}, props);
  }
  render() {
    return (
      <Scheduling
        ref={(el: Scheduling) => this.page = el}
        {...this.state}
        {...getPageRequiredProps()}
      />
    );
  }
}

function createIndexWrapper(config: SchedulingProps): Loader {
  return TestUtils.renderIntoDocument(
    <Loader {...config} />
  ) as Loader;
}

function emptyFn() { void(0); }

const defaultChannelId = "C1234";
const defaultUserId = "U1234";
const defaultTimeZone = "America/New_York";
const defaultTimeZoneName = "Eastern Time";

const emptyConfig: SchedulingProps = {
  scheduledActions: [],
  orgChannels: OrgChannels.fromJson({
    dmChannels: [],
    mpimChannels: [],
    orgSharedChannels: [],
    externallySharedChannels: [],
    teamChannels: []
  }),
  behaviorGroups: [],
  teamId: "1234",
  teamTimeZone: defaultTimeZone,
  teamTimeZoneName: defaultTimeZoneName,
  slackUserId: defaultUserId,
  slackBotUserId: "U5678",
  onSave: emptyFn,
  isSaving: false,
  onDelete: emptyFn,
  isDeleting: false,
  error: null,
  onClearErrors: emptyFn,
  justSavedAction: null,
  selectedScheduleId: null,
  filterChannelId: null,
  filterBehaviorGroupId: null,
  newAction: false,
  isAdmin: false,
  userMap: {},
  onLoadUserData: emptyFn,
  csrfToken: "FANCY_TOKEN",
  validTriggers: [],
  isValidatingTriggers: false,
  triggerValidationError: null
};

function newSchedule(props?: Partial<ScheduledActionInterface>) {
  return new ScheduledAction(Object.assign<ScheduledActionInterface, Partial<ScheduledActionInterface> | undefined>({
    id: ID.next(),
    scheduleType: "message",
    behaviorId: ID.next(),
    behaviorGroupId: ID.next(),
    trigger: ":tada:",
    arguments: [],
    recurrence: new Recurrence({
      timeZone: defaultTimeZone,
      timeZoneName: defaultTimeZoneName
    }),
    firstRecurrence: new Date('2017-10-14T13:00:00.000Z'),
    secondRecurrence: new Date('2017-10-15T13:00:00.000Z'),
    useDM: false,
    channel: defaultChannelId
  }, props));
}

function newChannel(props?: Partial<ScheduleChannelInterface>) {
  return new ScheduleChannel(Object.assign<ScheduleChannelInterface, Partial<ScheduleChannelInterface> | undefined>({
    id: defaultChannelId,
    name: "channel",
    context: "Slack",
    isBotMember: true,
    isSelfDm: false,
    isOtherDm: false,
    isPrivateChannel: false,
    isPrivateGroup: false,
    isArchived: false,
    isOrgShared: false,
    isExternallyShared: false,
    isReadOnly: false
  }, props));
}

describe('Scheduling', () => {
  describe('render', () => {
    it('renders an error message when there are no scheduled items and no channels', () => {
      const wrapper = createIndexWrapper(emptyConfig);
      const page = wrapper.page;
      const noScheduleSpy = jest.spyOn(page, 'renderNoSchedules');
      const errorMessageSpy = jest.spyOn(page, 'renderErrorMessage');
      const noScheduleMessageSpy = jest.spyOn(page, 'renderNoSchedulesMessage');
      const groupSpy = jest.spyOn(page, 'renderGroups');
      page.render();
      expect(noScheduleSpy).toHaveBeenCalled();
      expect(errorMessageSpy).toHaveBeenCalled();
      expect(noScheduleMessageSpy).not.toHaveBeenCalled();
      expect(groupSpy).not.toHaveBeenCalled();
    });

    it('renders the no schedules message with channels but with no scheduled items', () => {
      const wrapper = createIndexWrapper(Object.assign({}, emptyConfig, {
        orgChannels: emptyConfig.orgChannels.clone({
          teamChannels: [TeamChannels.fromJson({ teamName: "Test team", channelList: [newChannel()] })]
        })
      }));
      const page = wrapper.page;
      const noScheduleSpy = jest.spyOn(page, 'renderNoSchedules');
      const errorMessageSpy = jest.spyOn(page, 'renderErrorMessage');
      const noScheduleMessageSpy = jest.spyOn(page, 'renderNoSchedulesMessage');
      const groupSpy = jest.spyOn(page, 'renderGroups');
      page.render();
      expect(noScheduleSpy).toHaveBeenCalled();
      expect(errorMessageSpy).not.toHaveBeenCalled();
      expect(noScheduleMessageSpy).toHaveBeenCalled();
      expect(groupSpy).not.toHaveBeenCalled();
    });

    it('renders with some scheduled items', () => {
      const config = Object.assign<{}, SchedulingProps, Partial<SchedulingProps>>({}, emptyConfig, {
        scheduledActions: [newSchedule(), newSchedule()],
        orgChannels: emptyConfig.orgChannels.clone({
          teamChannels: [TeamChannels.fromJson({ teamName: "Test team", channelList: [newChannel()] })]
        })
      });
      const wrapper = createIndexWrapper(config);
      const page = wrapper.page;
      const noScheduleSpy = jest.spyOn(page, 'renderNoSchedules');
      const errorMessageSpy = jest.spyOn(page, 'renderErrorMessage');
      const noScheduleMessageSpy = jest.spyOn(page, 'renderNoSchedulesMessage');
      const groupSpy = jest.spyOn(page, 'renderGroups');
      page.render();
      expect(noScheduleSpy).not.toHaveBeenCalled();
      expect(errorMessageSpy).not.toHaveBeenCalled();
      expect(noScheduleMessageSpy).not.toHaveBeenCalled();
      expect(groupSpy).toHaveBeenCalled();
    });
  });

  describe('getInitialState', () => {
    it("doesn't set the selected item or filter channels if no selected item or channel ID is provided", () => {
      const channels = [newChannel()];
      const schedules = [newSchedule({
        channel: channels[0].id
      }), newSchedule()];
      const wrapper = createIndexWrapper(Object.assign({}, emptyConfig, {
        scheduledActions: schedules,
        orgChannels: emptyConfig.orgChannels.clone({
          teamChannels: [TeamChannels.fromJson({ teamName: "Test team", channelList: channels })]
        }),
        selectedScheduleId: null,
        filterChannelId: null
      }));
      const page = wrapper.page;
      expect(page.state.selectedItem).toBe(null);
      expect(page.state.isEditing).toBe(false);
      expect(page.state.filterChannelId).toBe("");
    });

    it('sets the selected item, in editing state', () => {
      const channels = [newChannel()];
      const schedules = [newSchedule({
        channel: channels[0].id
      }), newSchedule()];
      const wrapper = createIndexWrapper(Object.assign({}, emptyConfig, {
        scheduledActions: schedules,
        orgChannels: emptyConfig.orgChannels.clone({
          teamChannels: [{
            teamName: "Test team",
            channelList: channels
          }]
        }),
        selectedScheduleId: schedules[0].id,
        filterChannelId: null
      }));
      const page = wrapper.page;
      expect(page.state.selectedItem).toBe(schedules[0]);
      expect(page.state.isEditing).toBe(true);
      expect(page.state.filterChannelId).toEqual("");
    });

    it('sets the channel filter', () => {
      const channels = [newChannel()];
      const schedules = [newSchedule({
        channel: channels[0].id
      }), newSchedule()];
      const wrapper = createIndexWrapper(Object.assign({}, emptyConfig, {
        scheduledActions: schedules,
        orgChannels: emptyConfig.orgChannels.clone({
          teamChannels: [{
            teamName: "Test team",
            channelList: channels
          }]
        }),
        selectedScheduleId: null,
        filterChannelId: channels[0].id
      }));
      const page = wrapper.page;
      expect(page.state.selectedItem).toBe(null);
      expect(page.state.isEditing).toBe(false);
      expect(page.state.filterChannelId).toEqual(channels[0].id);
    });

    it('sets the selected item in editing state and the channel filter', () => {
      const channels = [newChannel()];
      const schedules = [newSchedule({
        channel: channels[0].id
      }), newSchedule()];
      const wrapper = createIndexWrapper(Object.assign({}, emptyConfig, {
        scheduledActions: schedules,
        orgChannels: emptyConfig.orgChannels.clone({
          teamChannels: [{
            teamName: "Test team",
            channelList: channels
          }]
        }),
        selectedScheduleId: schedules[0].id,
        filterChannelId: channels[0].id
      }));
      const page = wrapper.page;
      expect(page.state.selectedItem).toBe(schedules[0]);
      expect(page.state.isEditing).toBe(true);
      expect(page.state.filterChannelId).toEqual(channels[0].id);
    });

    it('sets the skill filter', () => {
      const channels = [newChannel()];
      const behaviorGroups: Array<BehaviorGroup> = [BehaviorGroup.fromJson({
        id: ID.next(),
        teamId: ID.next(),
        actionInputs: [],
        dataTypeInputs: [],
        behaviorVersions: [BehaviorVersion.fromJson({
          behaviorId: ID.next(),
          inputIds: [],
          triggers: [],
          config: BehaviorConfig.fromJson({
            responseTypeId: "Normal",
            isDataType: false,
            isTest: false
          }),
          functionBody: "",
          name: "MyAction"
        })],
        libraryVersions: [],
        requiredAWSConfigs: [],
        requiredOAuthApiConfigs: [],
        requiredSimpleTokenApis: [],
        isManaged: false
      })];
      const schedules = [newSchedule({
        channel: channels[0].id,
        behaviorGroupId: behaviorGroups[0].id,
        behaviorId: behaviorGroups[0].behaviorVersions[0].behaviorId,
        trigger: null
      })];
      const wrapper = createIndexWrapper(Object.assign({}, emptyConfig, {
        scheduledActions: schedules,
        orgChannels: emptyConfig.orgChannels.clone({
          teamChannels: [{
            teamName: "Test team",
            channelList: channels
          }]
        }),
        selectedScheduleId: null,
        filterChannelId: null,
        filterBehaviorGroupId: behaviorGroups[0].id
      }));
      const page = wrapper.page;
      expect(page.state.filterBehaviorGroupId).toEqual(behaviorGroups[0].id);
    });

  });

  describe('componentWillReceiveProps', () => {
    it('sets state to justSaved if current props isSaving is true, and new props isSaving is false', () => {
      const config = Object.assign<{}, SchedulingProps, Partial<SchedulingProps>>({}, emptyConfig, {
        isSaving: true
      });
      const wrapper = createIndexWrapper(config);
      const page = wrapper.page;
      const stateSpy = jest.spyOn(page, 'setState');
      wrapper.setState({
        isSaving: false
      });
      expect(stateSpy).toHaveBeenCalledWith({
        justSaved: true,
        justDeleted: false,
        isEditing: false,
        selectedItem: null
      });
    });

    it('sets state to justDeleted if current props isDeleting is true, and new props isDeleting is false', () => {
      const config = Object.assign<{}, SchedulingProps, Partial<SchedulingProps>>({}, emptyConfig, {
        isDeleting: true
      });
      const wrapper = createIndexWrapper(config);
      const page = wrapper.page;
      const stateSpy = jest.spyOn(page, 'setState');
      wrapper.setState({
        isDeleting: false
      });
      expect(stateSpy).toHaveBeenCalledWith({
        filterChannelId: "",
        selectedItem: null,
        justSaved: false,
        justDeleted: true,
        isEditing: false
      });
    });

    it('maintains a channel filter if there are any remaining actions for that channel', () => {
      expect.assertions(1);
      const channels = [newChannel()];
      const schedules = [newSchedule({
        channel: channels[0].id
      }), newSchedule({
        channel: channels[0].id
      })];
      const config = Object.assign({}, emptyConfig, {
        scheduledActions: schedules,
        orgChannels: emptyConfig.orgChannels.clone({
          teamChannels: [TeamChannels.fromJson({ teamName: "Test team", channelList: channels })]
        }),
        selectedScheduleId: schedules[0].id,
        isDeleting: true
      });
      const wrapper = createIndexWrapper(config);
      const page = wrapper.page;
      page.setState({
        selectedItem: schedules[0],
        filterChannelId: channels[0].id
      });
      const stateSpy = jest.spyOn(page, 'setState');
      wrapper.setState({
        scheduledActions: schedules.slice(1),
        isDeleting: false
      });
      expect(stateSpy).toHaveBeenCalledWith({
        filterChannelId: channels[0].id,
        selectedItem: null,
        justSaved: false,
        justDeleted: true,
        isEditing: false
      });
    });

    it('clears the channel filter if there are no remaining actions for that channel', () => {
      expect.assertions(1);
      const channels = [newChannel()];
      const schedules = [newSchedule({
        channel: channels[0].id
      })];
      const config = Object.assign<{}, SchedulingProps, Partial<SchedulingProps>>({}, emptyConfig, {
        scheduledActions: schedules,
        orgChannels: emptyConfig.orgChannels.clone({
          teamChannels: [TeamChannels.fromJson({ teamName: "Test team", channelList: channels })]
        }),
        selectedScheduleId: schedules[0].id,
        isDeleting: true
      });
      const wrapper = createIndexWrapper(config);
      const page = wrapper.page;
      page.setState({
        selectedItem: schedules[0],
        filterChannelId: channels[0].id
      });
      const stateSpy = jest.spyOn(page, 'setState');
      wrapper.setState({
        scheduledActions: [],
        isDeleting: false
      });
      expect(stateSpy).toHaveBeenCalledWith({
        filterChannelId: "",
        selectedItem: null,
        justSaved: false,
        justDeleted: true,
        isEditing: false
      });
    });
  });

  describe('componentWillUpdate', () => {
    it('clears the channel filter if a new skill ID is chosen and the current channel filter has no remaining actions', () => {
      const channels = [newChannel({
        id: ID.next()
      }), newChannel({
        id: ID.next()
      })];
      const schedules = [newSchedule({
        channel: channels[0].id,
        behaviorGroupId: "1"
      }), newSchedule({
        channel: channels[1].id,
        behaviorGroupId: "2"
      })];
      const config = Object.assign<{}, SchedulingProps, Partial<SchedulingProps>>({}, emptyConfig, {
        scheduledActions: schedules,
        orgChannels: emptyConfig.orgChannels.clone({
          teamChannels: [TeamChannels.fromJson({ teamName: "test", channelList: channels })]
        }),
        filterBehaviorGroupId: null,
        filterChannelId: channels[0].id
      });
      const wrapper = createIndexWrapper(config);
      const page = wrapper.page;
      expect(page.state.filterChannelId).toEqual(channels[0].id);
      page.setState({
        filterBehaviorGroupId: "1"
      });
      expect(page.state.filterChannelId).toEqual(channels[0].id);
      page.setState({
        filterBehaviorGroupId: ""
      });
      expect(page.state.filterChannelId).toEqual(channels[0].id);
      page.setState({
        filterBehaviorGroupId: "2"
      });
      expect(page.state.filterChannelId).toEqual("");
    });
  });

  describe('getScheduleByChannel', () => {
    it('returns scheduled actions grouped by channel, with channels and actions in a channel both sorted ascending by first recurrence', () => {
      const channel1 = newChannel({ name: "channel1", id: "channel1" });
      const channel2 = newChannel({ name: "channel2", id: "channel2" });
      const action1 = newSchedule({ firstRecurrence: new Date("2020-01-01T00:00:00.000Z"), channel: "channel1" });
      const action2 = newSchedule({ firstRecurrence: new Date("2018-01-01T00:00:00.000Z"), channel: "channel2" });
      const action3 = newSchedule({ firstRecurrence: new Date("2019-01-01T00:00:00.000Z"), channel: "channel1" });
      const action4 = newSchedule({ firstRecurrence: new Date("2020-01-01T00:00:00.000Z"), channel: "channel2" });
      const wrapper = createIndexWrapper(Object.assign<{}, SchedulingProps, Partial<SchedulingProps>>({}, emptyConfig, {
        scheduledActions: [action1, action2, action3, action4],
        orgChannels: emptyConfig.orgChannels.clone({
          teamChannels: [TeamChannels.fromJson({ teamName: "Test team", channelList: [channel1, channel2] })]
        }),
      }));
      const page = wrapper.page;
      const grouped = page.getScheduleByChannel();
      expect(grouped.map((ea) => ea.channelId)).toEqual(["channel2", "channel1"]);
      expect(grouped[0].actions).toEqual([action2, action4]);
      expect(grouped[1].actions).toEqual([action3, action1]);
    });
  })
});
