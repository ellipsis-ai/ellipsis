define(function(require) {
var React = require('react');

return React.createClass({
  displayName: 'BehaviorEditorCodeHeader',
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
                onChange={this.props.onParamChange.bind(this, index)}
                onDelete={this.props.onParamDelete.bind(this, index)}
                onEnterKey={this.props.onEnterKey.bind(this, index)}
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
            <button type="button" className="button-s" onClick={this.props.paramAdd}>Add parameter</button>
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