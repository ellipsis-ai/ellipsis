define(function(require) {
var React = require('react'),
  BehaviorEditorMixin = require('./behavior_editor_mixin'),
  BehaviorEditorHelpButton = require('./behavior_editor_help_button'),
  BehaviorEditorUserInputDefinition = require('./behavior_editor_user_input_definition');

return React.createClass({
  displayName: 'BehaviorEditorCodeHeader',
  mixins: [BehaviorEditorMixin],
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
    return this.props.hasParams ? this.props.params.length + 2 : 1;
  },
  boilerplateLine: function() {
    return this.props.hasParams ?
      (<span className="pll">{"onSuccess, onError, ellipsis "}</span>) :
      "function(onSuccess, onError, ellipsis) { ";
  },
  render: function() {
    return (
      <div className="border-top border-left border-right border-radius-top pvs">

        <div className={this.props.hasParams ? "" : "display-none"}>
          <div className="columns columns-elastic">
            <div className="column column-shrink plxxxl prn align-r position-relative">
              <code className="type-disabled type-s position-absolute position-top-right prxs">1</code>
            </div>
            <div className="column column-expand plxs">
              <code className="type-weak type-s">{"function ("}</code>
            </div>
          </div>
        </div>

        {this.props.params.map(function(param, paramIndex) {
          return (
            <div key={'paramContainer' + paramIndex} className="columns columns-elastic">
              <div className="column column-shrink plxxxl prn align-r position-relative">
                <code className="type-disabled type-s position-absolute position-top-right pts prxs">{paramIndex + 2}</code>
              </div>
              <div className="column column-expand pll">
                <BehaviorEditorUserInputDefinition
                  key={'BehaviorEditorUserInputDefinition' + paramIndex}
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

        <div className="columns columns-elastic">
          <div className="column column-expand">
            <div className="columns columns-elastic">
              <div className="column column-shrink plxxxl prn align-r position-relative">
                <code className="type-disabled type-s position-absolute position-top-right prxs">{this.boilerplateLineNumber()}</code>
              </div>
              <div className="column column-expand plxs">
                <code className="type-weak type-s">{this.boilerplateLine()}</code>
                <BehaviorEditorHelpButton onClick={this.props.onToggleHelp} toggled={this.props.helpVisible} />
              </div>
            </div>
          </div>
          <div className="column column-shrink pr-symbol align-r">
            <button type="button" className="button-s" onClick={this.props.onParamAdd}>Add parameter</button>
            <span className="button-symbol-placeholder"></span>
          </div>
        </div>

        <div className={this.props.hasParams ? "" : "display-none"}>
          <div className="columns columns-elastic">
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
