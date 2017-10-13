import React from 'react';
import TestUtils from 'react-addons-test-utils';
window.crypto = require('./mocks/mock_window_crypto');
global.fetch = require('./mocks/mock_fetch');

const Scheduling = require('../app/assets/javascripts/scheduling/index'),
  Recurrence = require('../app/assets/javascripts/models/recurrence'),
  ScheduledAction = require('../app/assets/javascripts/models/scheduled_action'),
  ScheduleChannel = require('../app/assets/javascripts/models/schedule_channel'),
  ID = require('../app/assets/javascripts/lib/id');

function createIndex(config) {
  return TestUtils.renderIntoDocument(
    <Scheduling {...config} />
  );
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
    it('renders with no scheduled items', () => {
      const page = createIndex(Object.assign({}, emptyConfig));
      const noScheduleSpy = jest.spyOn(page, 'renderNoSchedules');
      const groupSpy = jest.spyOn(page, 'renderGroups');
      page.render();
      expect(noScheduleSpy).toHaveBeenCalled();
      expect(groupSpy).not.toHaveBeenCalled();
    });

    it('renders with some scheduled items', () => {
      const page = createIndex(Object.assign({}, emptyConfig, {
        scheduledActions: [newSchedule(), newSchedule()],
        channelList: [newChannel()]
      }));
      const noScheduleSpy = jest.spyOn(page, 'renderNoSchedules');
      const groupSpy = jest.spyOn(page, 'renderGroups');
      page.render();
      expect(noScheduleSpy).not.toHaveBeenCalled();
      expect(groupSpy).toHaveBeenCalled();
    });
  });

  describe('getInitialState', () => {
    it("doesn't set the selected item or filter channels if no selected item is provided", () => {
      const channels = [newChannel()];
      const schedules = [newSchedule({
        channel: channels[0].id
      }), newSchedule()];
      const page = createIndex(Object.assign({}, emptyConfig, {
        scheduledActions: schedules,
        channelList: channels,
        selectedScheduleId: null
      }));
      expect(page.state.selectedItem).toBe(null);
      expect(page.state.isEditing).toBe(false);
      expect(page.state.filterChannelId).toBe(null);
    });

    it('sets the selected item, in editing state, with the channels filtered to that channel', () => {
      const channels = [newChannel()];
      const schedules = [newSchedule({
        channel: channels[0].id
      }), newSchedule()];
      const page = createIndex(Object.assign({}, emptyConfig, {
        scheduledActions: schedules,
        channelList: channels,
        selectedScheduleId: schedules[0].id
      }));
      expect(page.state.selectedItem).toBe(schedules[0]);
      expect(page.state.isEditing).toBe(true);
      expect(page.state.filterChannelId).toEqual(channels[0].id);
    });
  });
});
