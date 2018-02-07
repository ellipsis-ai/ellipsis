import * as React from 'react';
import TestUtils from 'react-addons-test-utils';
import mockFetch from './../../../mocks/mock_fetch';
global.fetch = mockFetch;
jest.mock('../../../../app/assets/frontend/lib/browser_utils');
import Scheduling from '../../../../app/assets/frontend/scheduling/index';
import Recurrence from '../../../../app/assets/frontend/models/recurrence';
import ScheduledAction from '../../../../app/assets/frontend/models/scheduled_action';
import ScheduleChannel from '../../../../app/assets/frontend/models/schedule_channel';
import ID from '../../../../app/assets/frontend/lib/id';

jsRoutes.controllers.ScheduledActionsController.index = () => ({ url: "/test" });

class Loader extends React.Component {
  constructor(props) {
    super(props);
    this.state = props;
  }
  render() {
    return (
      <Scheduling ref={(el) => this.page = el} {...this.state} />
    );
  }
}

function createIndexWrapper(config) {
  const wrapper = TestUtils.renderIntoDocument(
    <Loader {...config} />
  );
  return wrapper;
}

function emptyFn() { void(0); }

const defaultChannelId = "C1234";
const defaultUserId = "U1234";
const defaultTimeZone = "America/New_York";
const defaultTimeZoneName = "Eastern Time";

const emptyConfig = Object.freeze({
  scheduledActions: [],
  channelList: [],
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
  newAction: false
});

function newSchedule(props) {
  return new ScheduledAction(Object.assign({
    id: ID.next(),
    scheduleType: "message",
    behaviorId: ID.next(),
    behaviorGroupId: ID.next(),
    trigger: ":tada:",
    arguments: {},
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

function newChannel(props) {
  return new ScheduleChannel(Object.assign({
    id: defaultChannelId,
    name: "test",
    context: "Slack",
    members: [defaultUserId],
    isPublic: false,
    isArchived: false
  }, props));
}

describe('Scheduling', () => {
  describe('render', () => {
    it('renders an error message when there are no scheduled items and no channels', () => {
      const wrapper = createIndexWrapper(Object.assign({}, emptyConfig, {
        channelList: null
      }));
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
        channelList: [newChannel()]
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
      const wrapper = createIndexWrapper(Object.assign({}, emptyConfig, {
        scheduledActions: [newSchedule(), newSchedule()],
        channelList: [newChannel()]
      }));
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
    it("doesn't set the selected item or filter channels if no selected item is provided", () => {
      const channels = [newChannel()];
      const schedules = [newSchedule({
        channel: channels[0].id
      }), newSchedule()];
      const wrapper = createIndexWrapper(Object.assign({}, emptyConfig, {
        scheduledActions: schedules,
        channelList: channels,
        selectedScheduleId: null
      }));
      const page = wrapper.page;
      expect(page.state.selectedItem).toBe(null);
      expect(page.state.isEditing).toBe(false);
      expect(page.state.filterChannelId).toBe(null);
    });

    it('sets the selected item, in editing state, with the channels filtered to that channel', () => {
      const channels = [newChannel()];
      const schedules = [newSchedule({
        channel: channels[0].id
      }), newSchedule()];
      const wrapper = createIndexWrapper(Object.assign({}, emptyConfig, {
        scheduledActions: schedules,
        channelList: channels,
        selectedScheduleId: schedules[0].id
      }));
      const page = wrapper.page;
      expect(page.state.selectedItem).toBe(schedules[0]);
      expect(page.state.isEditing).toBe(true);
      expect(page.state.filterChannelId).toEqual(channels[0].id);
    });
  });

  describe('componentWillReceiveProps', () => {
    it('sets state to justSaved if current props isSaving is true, and new props isSaving is false', () => {
      const config = Object.assign({}, emptyConfig, {
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
      const config = Object.assign({}, emptyConfig, {
        isDeleting: true
      });
      const wrapper = createIndexWrapper(config);
      const page = wrapper.page;
      const stateSpy = jest.spyOn(page, 'setState');
      wrapper.setState({
        isDeleting: false
      });
      expect(stateSpy).toHaveBeenCalledWith({
        filterChannelId: null,
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
        channelList: channels,
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
      const config = Object.assign({}, emptyConfig, {
        scheduledActions: schedules,
        channelList: channels,
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
        filterChannelId: null,
        selectedItem: null,
        justSaved: false,
        justDeleted: true,
        isEditing: false
      });
    });
  });
});