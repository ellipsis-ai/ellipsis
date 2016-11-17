define(function(require) {
  var React = require('react'),
    Checklist = require('./checklist'),
    HelpButton = require('../help/help_button'),
    SectionHeading = require('./section_heading'),
    TriggerInput = require('./trigger_input'),
    Trigger = require('../models/trigger');

  return React.createClass({
    propTypes: {
      isFinishedBehavior: React.PropTypes.bool.isRequired,
      triggers: React.PropTypes.arrayOf(React.PropTypes.instanceOf(Trigger)).isRequired,
      onToggleHelp: React.PropTypes.func.isRequired,
      helpVisible: React.PropTypes.bool.isRequired,
      onTriggerAdd: React.PropTypes.func.isRequired,
      onTriggerChange: React.PropTypes.func.isRequired,
      onTriggerDelete: React.PropTypes.func.isRequired,
      onTriggerDropdownToggle: React.PropTypes.func.isRequired,
      openDropdownName: React.PropTypes.string.isRequired
    },

    hasMultipleTriggers: function() {
      return this.props.triggers.length > 1;
    },

    hasPrimaryTrigger: function() {
      return !!(this.props.triggers.length > 0 && this.props.triggers[0].text);
    },

    triggersUseParams: function() {
      return this.props.triggers.some((trigger) => trigger.hasNonRegexParams());
    },

    onTriggerEnterKey: function(index) {
      if (index + 1 < this.props.triggers.length) {
        this.focusOnTriggerIndex(index + 1);
      } else if (this.props.triggers[index].text) {
        this.addTrigger();
      }
    },

    focusOnTriggerIndex: function(index) {
      this.refs['trigger' + index].focus();
    },

    focusOnFirstBlankTrigger: function() {
      var blankTrigger = Object.keys(this.refs).find(function(key) {
        return key.match(/^trigger\d+$/) && this.refs[key].isEmpty();
      }, this);
      if (blankTrigger) {
        this.refs[blankTrigger].focus();
      }
    },

    addTrigger: function() {
      this.props.onTriggerAdd(this.focusOnFirstBlankTrigger);
    },

    changeTrigger: function(index, newTrigger) {
      this.props.onTriggerChange(index, newTrigger);
    },

    deleteTrigger: function(index) {
      this.props.onTriggerDelete(index);
    },

    toggleDropdown: function(dropdownName) {
      this.props.onTriggerDropdownToggle(dropdownName);
    },

    render: function() {
      return (
        <div className="columns">
          <div className="column column-one-quarter mobile-column-full mts mbxxl mobile-mbs">
            <SectionHeading>When someone says</SectionHeading>

            <Checklist disabledWhen={this.props.isFinishedBehavior}>
              <Checklist.Item checkedWhen={this.hasPrimaryTrigger()} hiddenWhen={this.props.isFinishedBehavior}>
                Write a question or phrase people should use to trigger a response.
              </Checklist.Item>
              <Checklist.Item checkedWhen={this.triggersUseParams()}>
                <span>A trigger can include “fill-in-the-blank” inputs, e.g. <code className="plxs">{"Call me {name}"}</code></span>
                <span className="pls">
                  <HelpButton onClick={this.props.onToggleHelp} toggled={this.props.helpVisible} />
                </span>
              </Checklist.Item>
            </Checklist>

          </div>
          <div className="column column-three-quarters mobile-column-full pll mobile-pln mbxxl">
            <div className="mbm">
              {this.props.triggers.map(function(trigger, index) {
                return (
                  <div key={`trigger${index}`}>
                    <TriggerInput
                      dropdownIsOpen={this.props.openDropdownName === `BehaviorEditorTriggerDropdown${index}`}
                      key={`BehaviorEditorTrigger${index}`}
                      id={`trigger${index}`}
                      ref={`trigger${index}`}
                      trigger={trigger}
                      hideDelete={!this.hasMultipleTriggers()}
                      onChange={this.changeTrigger.bind(this, index)}
                      onDelete={this.deleteTrigger.bind(this, index)}
                      onEnterKey={this.onTriggerEnterKey.bind(this, index)}
                      onHelpClick={this.props.onToggleHelp}
                      onToggleDropdown={this.toggleDropdown.bind(this, `BehaviorEditorTriggerDropdown${index}`)}
                      helpVisible={this.props.helpVisible}
                    />
                    {index + 1 < this.props.triggers.length ? (
                      <div className="pvxs type-label type-disabled align-c">or</div>
                    ) : null}
                  </div>
                );
              }, this)}
              <div className="mtm">
                <button type="button" className="button-s" onClick={this.addTrigger}>Add another trigger</button>
              </div>
            </div>
          </div>
        </div>
      );
    }
  });
});
