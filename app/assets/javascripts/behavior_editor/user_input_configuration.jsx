define(function(require) {
  var React = require('react'),
    SectionHeading = require('./section_heading'),
    UserInputDefinition = require('./user_input_definition'),
    Checklist = require('./checklist'),
    Collapsible = require('../shared_ui/collapsible'),
    BehaviorVersion = require('../models/behavior_version'),
    Param = require('../models/param'),
    Trigger = require('../models/trigger');

  return React.createClass({
    displayName: 'UserInputConfiguration',
    propTypes: {
      onParamChange: React.PropTypes.func.isRequired,
      onParamDelete: React.PropTypes.func.isRequired,
      onParamAdd: React.PropTypes.func.isRequired,
      onParamNameFocus: React.PropTypes.func.isRequired,
      onParamNameBlur: React.PropTypes.func.isRequired,
      onEnterKey: React.PropTypes.func.isRequired,
      userParams: React.PropTypes.arrayOf(React.PropTypes.instanceOf(Param)).isRequired,
      paramTypes: React.PropTypes.arrayOf(
        React.PropTypes.shape({
          id: React.PropTypes.string.isRequired,
          name: React.PropTypes.string.isRequired
        })
      ).isRequired,
      triggers: React.PropTypes.arrayOf(React.PropTypes.instanceOf(Trigger)).isRequired,
      isFinishedBehavior: React.PropTypes.bool.isRequired,
      behaviorHasCode: React.PropTypes.bool.isRequired,
      hasSharedAnswers: React.PropTypes.bool.isRequired,
      otherBehaviorsInGroup: React.PropTypes.arrayOf(React.PropTypes.instanceOf(BehaviorVersion)).isRequired,
      onToggleSharedAnswer: React.PropTypes.func.isRequired,
      savedAnswers: React.PropTypes.arrayOf(
        React.PropTypes.shape({
          inputId: React.PropTypes.string.isRequired,
          userAnswerCount: React.PropTypes.number.isRequired,
          myValueString: React.PropTypes.string
        })
      ).isRequired,
      onToggleSavedAnswer: React.PropTypes.func.isRequired,
      animationDisabled: React.PropTypes.bool,
      onConfigureType: React.PropTypes.func.isRequired
    },

    onChange: function(index, data) {
      this.props.onParamChange(index, data);
    },
    onDelete: function(index) {
      this.props.onParamDelete(index);
    },
    onEnterKey: function(index) {
      this.props.onEnterKey(index);
    },

    onNameFocus: function(index) {
      this.props.onParamNameFocus(index);
    },

    onNameBlur: function(index) {
      this.props.onParamNameBlur(index);
    },

    focusIndex: function(index) {
      this.refs['param' + index].focus();
    },

    hasParams: function() {
      return this.props.userParams.length > 0;
    },

    isShared: function(param) {
      const firstBehaviorWithSameInput = this.props.otherBehaviorsInGroup.find(behavior => {
        const inputIds = behavior.params.map(ea => ea.inputVersionId);
        return inputIds.indexOf(param.inputVersionId) !== -1;
      });
      return !!firstBehaviorWithSameInput;
    },

    countLinkedTriggersForParam: function(paramName, paramIndex) {
      return this.props.triggers.filter((trigger) => trigger.usesParamName(paramName) || trigger.capturesParamIndex(paramIndex)).length;
    },

    hasLinkedTriggers: function() {
      return this.props.userParams.some((param) => {
        return this.props.triggers.some((trigger) => trigger.usesParamName(param.name));
      });
    },

    hasRegexCapturingTriggers: function() {
      return this.props.triggers.some((trigger) => trigger.hasRegexCapturingParens());
    },

    hasRegexTriggers: function() {
      return this.props.triggers.some((trigger) => trigger.isRegex);
    },

    getSavedAnswersFor: function(inputId) {
      return this.props.savedAnswers.find((answers) => answers.inputId === inputId);
    },

    renderReuseParameter: function(optionalProperties) {
      var props = Object.assign({}, optionalProperties);
      if (this.props.hasSharedAnswers) {
        return (
          <button type="button"
            className={"button-s " + (props.className || "")}
            onClick={this.props.onToggleSharedAnswer}
          >
            Use a saved answer from another action…
          </button>
        );
      } else {
        return null;
      }
    },

    render: function() {
      return (
        <div>
          <Collapsible revealWhen={!this.hasParams()} animationDisabled={this.props.animationDisabled}>
            <div className="bg-blue-lighter border-top border-blue ptl pbs">
              <div className="container container-wide">
                <div className="columns columns-elastic narrow-columns-float">
                  <div className="column column-expand">
                    <p className="mbs">
                      <span>You can add inputs to ask for additional information from the user, or </span>
                      <span>to clarify what kind of input will come from the trigger.</span>
                    </p>
                  </div>
                  <div className="column column-shrink align-r align-m narrow-align-l display-ellipsis mobile-display-no-ellipsis">
                    <button type="button" className="button-s mbs mobile-mrm" onClick={this.props.onParamAdd}>Add an input</button>
                    {this.renderReuseParameter({ className: "mlm mobile-mln mbs" })}
                  </div>
                </div>
              </div>
            </div>
          </Collapsible>

          <Collapsible revealWhen={this.hasParams()} animationDisabled={this.props.animationDisabled}>

            <hr className="mtn thin bg-gray-light" />

            <div className="columns container container-narrow">
              <div className="mbxxl">
                <div>
                  <SectionHeading number="2">Collect input</SectionHeading>
                  <div>
                    <Checklist disabledWhen={this.props.isFinishedBehavior}>
                      <Checklist.Item hiddenWhen={this.props.isFinishedBehavior} checkedWhen={this.props.behaviorHasCode}>
                        <span>If the action runs code, each input will be sent to the function as a parameter </span>
                        <span>with the same name.</span>
                      </Checklist.Item>
                      <Checklist.Item checkedWhen={this.hasLinkedTriggers()}>
                        <span>User input can also come from triggers that include matching fill-in-the-blank </span>
                        <code>{"{labels}"}</code>
                      </Checklist.Item>
                      <Checklist.Item hiddenWhen={!this.hasRegexTriggers()} checkedWhen={this.hasRegexCapturingTriggers()}>
                        <span>Regex triggers will send text captured in parentheses in the same order as </span>
                        <span>the inputs are defined.</span>
                      </Checklist.Item>
                    </Checklist>
                  </div>
                  <div className="mbm">
                    {this.props.userParams.map((param, paramIndex) => (
                      <div key={`userParam${paramIndex}`}>
                        <UserInputDefinition
                          key={'UserInputDefinition' + paramIndex}
                          ref={'param' + paramIndex}
                          param={param}
                          isShared={this.isShared(param)}
                          paramTypes={this.props.paramTypes}
                          onChange={this.onChange.bind(this, paramIndex)}
                          onDelete={this.onDelete.bind(this, paramIndex)}
                          onEnterKey={this.onEnterKey.bind(this, paramIndex)}
                          onNameFocus={this.onNameFocus.bind(this, paramIndex)}
                          onNameBlur={this.onNameBlur.bind(this, paramIndex)}
                          numLinkedTriggers={this.countLinkedTriggersForParam(param.name, paramIndex)}
                          id={paramIndex}
                          savedAnswers={this.getSavedAnswersFor(param.inputId)}
                          onToggleSavedAnswer={this.props.onToggleSavedAnswer}
                          onConfigureType={this.props.onConfigureType}
                        />
                        {paramIndex + 1 < this.props.userParams.length ? (
                          <div className="pvxs type-label type-disabled align-c">and</div>
                        ) : null}
                      </div>
                    ))}
                  </div>
                  <div>
                    <button type="button" className="button-s mrm mbs" onClick={this.props.onParamAdd}>
                      Add another input
                    </button>
                    {this.renderReuseParameter({ className: "mbs" })}
                  </div>
                </div>
              </div>
            </div>
          </Collapsible>
        </div>

      );
    }
  });
});
