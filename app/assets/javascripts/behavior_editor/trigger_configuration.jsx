define(function(require) {
  var React = require('react'),
    Checklist = require('./checklist'),
    HelpButton = require('../help/help_button'),
    SectionHeading = require('./section_heading'),
    TriggerInput = require('./trigger_input');

  return React.createClass({
    propTypes: {
      isFinishedBehavior: React.PropTypes.bool.isRequired,
      triggers: React.PropTypes.arrayOf(React.PropTypes.shape({
        caseSensitive: React.PropTypes.bool.isRequired,
        isRegex: React.PropTypes.bool.isRequired,
        requiresMention: React.PropTypes.bool.isRequired,
        text: React.PropTypes.string.isRequired
      })).isRequired,
      onToggleHelp: React.PropTypes.func.isRequired,
      helpVisible: React.PropTypes.bool.isRequired,
      onTriggerAdd: React.PropTypes.func.isRequired,
      onTriggerChange: React.PropTypes.func.isRequired,
      onTriggerDelete: React.PropTypes.func.isRequired
    },

    hasMultipleTriggers: function() {
      return this.props.triggers.length > 1;
    },

    hasPrimaryTrigger: function() {
      return !!(this.props.triggers.length > 0 && this.props.triggers[0].text);
    },

    triggersUseParams: function() {
      return this.props.triggers.some(function(trigger) {
        return trigger.text.match(/{.+}/);
      });
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

    render: function() {
      return (
        <div className="columns">
          <div className="column column-one-quarter mobile-column-full mts mbxxl mobile-mbs">
            <SectionHeading>When someone says</SectionHeading>

            <Checklist disabledWhen={this.props.isFinishedBehavior}>
              <Checklist.Item checkedWhen={this.hasPrimaryTrigger()} hiddenWhen={this.props.isFinishedBehavior}>
                Write a question or phrase people should use to trigger a response.
              </Checklist.Item>
              <Checklist.Item checkedWhen={this.hasMultipleTriggers()} hiddenWhen={this.props.isFinishedBehavior && this.hasMultipleTriggers()}>
                You can add multiple triggers.
              </Checklist.Item>
              <Checklist.Item checkedWhen={this.triggersUseParams()}>
                <span>A trigger can include “fill-in-the-blank” parts, e.g. <code className="plxs">{"Call me {name}"}</code></span>
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
                  <TriggerInput
                    large={index === 0}
                    includeHelp={index === 0}
                    key={"BehaviorEditorTrigger" + index}
                    id={"trigger" + index}
                    ref={"trigger" + index}
                    value={trigger.text}
                    requiresMention={trigger.requiresMention}
                    isRegex={trigger.isRegex}
                    caseSensitive={trigger.caseSensitive}
                    hideDelete={!this.hasMultipleTriggers()}
                    onChange={this.changeTrigger.bind(this, index)}
                    onDelete={this.deleteTrigger.bind(this, index)}
                    onEnterKey={this.onTriggerEnterKey.bind(this, index)}
                    onHelpClick={this.props.onToggleHelp}
                    helpVisible={this.props.helpVisible}
                  />
                );
              }, this)}
            </div>
            <div className="prsymbol mobile-prn align-r mobile-align-l">
              <button type="button" className="button-s" onClick={this.addTrigger}>Add another trigger</button>
            </div>
          </div>
        </div>
      );
    }
  });
});
