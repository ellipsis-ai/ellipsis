jest.unmock('../app/assets/javascripts/behavior_editor.jsx');

var React = require('react');
var BehaviorEditor = require('../app/assets/javascripts/behavior_editor.jsx');
var requirejs = require('requirejs');

describe('BehaviorEditor', () => {
  it('runs tests', () => {
    expect(BehaviorEditor).toBeTruthy()
  });
});
