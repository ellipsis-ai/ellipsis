import React from 'react';
import TestUtils from 'react-addons-test-utils';
window.crypto = require('./mocks/mock_window_crypto');
global.fetch = require('./mocks/mock_fetch');

const Scheduling = require('../app/assets/javascripts/scheduling/index'),
  ScheduledAction = require('../app/assets/javascripts/models/scheduled_action'),
  ScheduleChannel = require('../app/assets/javascripts/models/schedule_channel'),
  BehaviorGroup = require('../app/assets/javascripts/models/behavior_group');

function createIndex(config) {
  return TestUtils.renderIntoDocument(
    <Scheduling {...config} />
  );
}

const emptyFn = function() { void(0); }

const defaultConfig = Object.freeze({
  scheduledActions: [],
  channelList: [],
  behaviorGroups: [],
  teamTimeZone: "America/New_York",
  teamTimeZoneName: "Eastern Time",
  slackUserId: "U1234",
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

describe('Scheduling', () => {
  it('renders', () => {
    const page = createIndex(Object.assign({}, defaultConfig));
    page.render();
  });
});
