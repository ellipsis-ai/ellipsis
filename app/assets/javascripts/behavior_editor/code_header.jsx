define(function(require) {
var React = require('react'),
  BehaviorEditorMixin = require('./behavior_editor_mixin'),
  HelpButton = require('./help_button'),
  UserInputDefinition = require('./user_input_definition');

return React.createClass({
  mixins: [BehaviorEditorMixin],
  propTypes: {
    shouldExpandParams: React.PropTypes.bool,
    helpVisible: React.PropTypes.bool,
    onEnterKey: React.PropTypes.func.isRequired,
    onParamAdd: React.PropTypes.func.isRequired,
    onParamChange: React.PropTypes.func.isRequired,
    onParamDelete: React.PropTypes.func.isRequired,
    onToggleHelp: React.PropTypes.func.isRequired,
    userParams: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
    systemParams: React.PropTypes.arrayOf(React.PropTypes.string).isRequired,
    apiParams: React.PropTypes.arrayOf(React.PropTypes.string).isRequired
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
  focusIndex: function(index) {
    this.refs['param' + index].focus();
  },
  boilerplateLineNumber: function() {
    return this.props.shouldExpandParams ? this.props.userParams.length + 2 : 1;
  },
  boilerplateLine: function() {
    var systemParamString = this.props.systemParams.join(", ");
    if (this.props.apiParams.length > 0) {
      systemParamString += ", ";
    }
    var apiParamString = this.props.apiParams.join(", ");
    if (this.props.shouldExpandParams) {
      return (
        <span className="plm">
          <span>{systemParamString} </span>
          <span className="type-black">{apiParamString} </span>
        </span>
      );
    } else {
      return (
        <span>
          <span>function(</span>
          <span>{systemParamString}</span>
          <span className="type-black">{apiParamString}</span>
          <span>{") { "}</span>
        </span>
      );
    }
  },
  render: function() {
    return (
      <div>

        <div className={this.props.shouldExpandParams ? "" : "display-none"}>
          <div className="columns columns-elastic">
            <div className="column column-shrink plxxxl prn align-r position-relative">
              <code className="type-disabled type-s position-absolute position-top-right prxs">1</code>
            </div>
            <div className="column column-expand plxs">
              <code className="type-weak type-s">{"function ("}</code>
            </div>
          </div>
        </div>

        {this.props.userParams.map(function(param, paramIndex) {
          return (
            <div key={'paramContainer' + paramIndex} className="columns columns-elastic">
              <div className="column column-shrink plxxxl prn align-r position-relative">
                <code className="type-disabled type-s position-absolute position-top-right pts prxs">{paramIndex + 2}</code>
              </div>
              <div className="column column-expand pll">
                <UserInputDefinition
                  key={'UserInputDefinition' + paramIndex}
                  ref={'param' + paramIndex}
                  name={param.name}
                  question={param.question}
                  onChange={this.onChange.bind(this, paramIndex)}
                  onDelete={this.onDelete.bind(this, paramIndex)}
                  onEnterKey={this.onEnterKey.bind(this, paramIndex)}
                  id={paramIndex}
                />
              </div>
            </div>
          );
        }, this)}

        <div className="columns">
          <div className="column column-right align-r prxs mobile-align-l mbs">
            <button type="button" className="button-s" onClick={this.props.onParamAdd}>Add parameter</button>
            <span className="button-symbol-placeholder" />
          </div>
          <div className="column">
            <div className="columns columns-elastic">
              <div className="column column-shrink plxxxl prn align-r position-relative">
                <code className="type-disabled type-s position-absolute position-top-right prxs">{this.boilerplateLineNumber()}</code>
              </div>
              <div className="column plxs">
                <code className="type-weak type-s">{this.boilerplateLine()}</code>
                <HelpButton onClick={this.props.onToggleHelp} toggled={this.props.helpVisible} />
              </div>
            </div>
          </div>
        </div>

        <div className={this.props.shouldExpandParams ? "" : "display-none"}>
          <div className="columns columns-elastic pbs">
            <div className="column column-shrink plxxxl prn align-r position-relative">
              <code className="type-disabled type-s position-absolute position-top-right prxs">{(this.boilerplateLineNumber() + 1)}</code>
            </div>
            <div className="column column-expand plxs">
              <code className="type-weak type-s">{") {"}</code>
            </div>
          </div>
        </div>

      </div>
    );
  }
});

});
