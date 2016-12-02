define(function(require) {
  var React = require('react'),
    SectionHeading = require('./section_heading'),
    DropdownMenu = require('../dropdown_menu'),
    UserInputDefinition = require('./user_input_definition'),
    Checklist = require('./checklist'),
    Collapsible = require('../collapsible'),
    Param = require('../models/param'),
    Trigger = require('../models/trigger');

  return React.createClass({
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
      otherParametersInGroup: React.PropTypes.arrayOf(React.PropTypes.instanceOf(Param)),
      toggleReuseParamDropdown: React.PropTypes.func.isRequired,
      openReuseParamDropdownWhen: React.PropTypes.bool.isRequired,
      onReuseParam: React.PropTypes.func.isRequired
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

    renderReuseParamLabel: function(param) {
      return (
        <div className="columns columns-elastic">
          <div className="type-bold column column-shrink">{param.name}</div>
          <div className="column column-expand">{param.question}</div>
        </div>
      );
    },

    onReuseParam: function(param) {
      this.props.onReuseParam(param);
    },

    renderReuseParamOption: function(param, index) {
      return (
        <DropdownMenu.Item
          key={`reuse-param-option-${index}`}
          onClick={this.onReuseParam.bind(this, param)}
          label={this.renderReuseParamLabel(param)}
        />
      );
    },

    renderReuseParameter: function() {
      return (
        <DropdownMenu
          label="Re-use an input from another action"
          labelClassName="button-s button-color"
          toggle={this.props.toggleReuseParamDropdown}
          openWhen={this.props.openReuseParamDropdownWhen}
        >
          {this.props.otherParametersInGroup.map( (ea, i) => this.renderReuseParamOption(ea, i))}
        </DropdownMenu>
      );
    },

    render: function() {
      return (
        <div>
          <Collapsible revealWhen={!this.hasParams()}>
            <div className="bg-blue-lighter border-top border-blue pvl">
              <div className="container">
                <div className="columns columns-elastic mobile-columns-float">
                  <div className="column column-expand">
                    <p className="mbn">
                      <span>You can add inputs to ask for additional information from the user, or </span>
                      <span>to clarify what kind of input will come from the trigger.</span>
                    </p>
                  </div>
                  <div className="column column-shrink align-m mobile-mtm">
                    <button type="button" className="button-s" onClick={this.props.onParamAdd}>Add inputs</button>
                  </div>
                </div>
              </div>
            </div>
          </Collapsible>

          <Collapsible revealWhen={this.hasParams()}>

            <hr className="mtn full-bleed thin bg-gray-light" />

            <div className="columns container">
              <div className="column column-one-quarter mobile-column-full mbxxl mobile-mbs">
                <SectionHeading number="2">Collect input</SectionHeading>

                <Checklist disabledWhen={this.props.isFinishedBehavior}>
                  <Checklist.Item hiddenWhen={this.props.isFinishedBehavior} checkedWhen={this.props.behaviorHasCode}>
                    <span>If the skill runs code, each input will be sent to the function as a parameter </span>
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
              <div className="column column-three-quarters mobile-column-full pll mobile-pln mbxxl">
                <div>
                  <div className="mbm">
                    {this.props.userParams.map((param, paramIndex) => (
                      <div key={`userParam${paramIndex}`}>
                        <UserInputDefinition
                          key={'UserInputDefinition' + paramIndex}
                          ref={'param' + paramIndex}
                          param={param}
                          paramTypes={this.props.paramTypes}
                          onChange={this.onChange.bind(this, paramIndex)}
                          onDelete={this.onDelete.bind(this, paramIndex)}
                          onEnterKey={this.onEnterKey.bind(this, paramIndex)}
                          onNameFocus={this.onNameFocus.bind(this, paramIndex)}
                          onNameBlur={this.onNameBlur.bind(this, paramIndex)}
                          numLinkedTriggers={this.countLinkedTriggersForParam(param.name, paramIndex)}
                          id={paramIndex}
                        />
                        {paramIndex + 1 < this.props.userParams.length ? (
                          <div className="pvxs type-label type-disabled align-c">and</div>
                        ) : null}
                      </div>
                    ))}
                  </div>
                  <div>
                    <button type="button" className="button-s mrm" onClick={this.props.onParamAdd}>
                      Add another input
                    </button>
                    {this.renderReuseParameter()}
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
