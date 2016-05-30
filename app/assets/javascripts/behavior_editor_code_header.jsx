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
      "function(onSuccess, onError, ellipis) { ";
  },
  render: function() {
    return (
      <div className="border-top border-left border-right border-radius-top pvs">

        <div className={this.props.hasParams ? "" : "display-none"}>
          <div className="columns columns-elastic">
            <div className="column column-shrink prs">
              <code className="type-disabled type-s">{this.padSpace(1, 3)}</code>
            </div>
            <div className="column column-expand">
              <code className="type-weak type-s">{"function ("}</code>
            </div>
          </div>
        </div>

        {this.props.params.map(function(param, paramIndex) {
          return (
            <div ref={'paramContainer' + paramIndex} className="columns columns-elastic">
              <div className="column column-shrink pts prs">
                <code className="type-disabled type-s">{this.padSpace(paramIndex + 2, 3)}</code>
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
              <div className="column column-shrink prs">
                <code className="type-disabled type-s">{this.padSpace(this.boilerplateLineNumber(), 3)}</code>
              </div>
              <div className="column column-expand">
                <code className="type-weak type-s">{this.boilerplateLine()}</code>
                <span className={this.visibleWhen(!this.props.helpVisible)}>
                  <BehaviorEditorHelpButton onClick={this.props.onToggleHelp} />
                </span>
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
            <div className="column column-shrink prs">
              <code className="type-disabled type-s">{this.padSpace(this.boilerplateLineNumber() + 1, 3)}</code>
            </div>
            <div className="column column-expand">
              <code className="type-weak type-s">{") {"}</code>
            </div>
          </div>
        </div>

      </div>
    );
  }
});

});
