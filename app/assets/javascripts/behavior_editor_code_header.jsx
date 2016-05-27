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
  render: function() {
    return (
      <div className="border-top border-left border-right border-radius-top pvs plxl">

        <div className={this.props.hasParams ? "" : "display-none"}>
          <code className="type-weak type-s">{"function ("}</code>
        </div>

        <div className="plxl">
          {this.props.params.map(function(param, index) {
            return (
              <BehaviorEditorUserInputDefinition
                key={'BehaviorEditorUserInputDefinition' + index}
                ref={'param' + index}
                name={param.name}
                question={param.question}
                onChange={this.onChange.bind(this, index)}
                onDelete={this.onDelete.bind(this, index)}
                onEnterKey={this.onEnterKey.bind(this, index)}
                id={index}
              />
            );
          }, this)}
        </div>

        <div className="columns columns-elastic">
          <div className="column column-expand">
            {this.props.hasParams ?
              (<code className="type-weak type-s plxl">{"onSuccess, onError, ellipsis "}</code>) :
              (<code className="type-weak type-s">{"function(onSuccess, onError, ellipis) { "}</code>)
            }
            <span className={this.visibleWhen(!this.props.helpVisible)}>
              <BehaviorEditorHelpButton onClick={this.props.onToggleHelp} />
            </span>
          </div>
          <div className="column column-shrink pr-symbol align-r">
            <button type="button" className="button-s" onClick={this.props.onParamAdd}>Add parameter</button>
            <span className="button-symbol-placeholder"></span>
          </div>
        </div>

        <div className={this.props.hasParams ? "" : "display-none"}>
          <code className="type-weak type-s">{") {"}</code>
        </div>

      </div>
    );
  }
});

});
