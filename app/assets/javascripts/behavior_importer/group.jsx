define(function(require) {
  var React = require('react'),
    Behavior = require('./behavior');

  return React.createClass({
    propTypes: {
      behaviors: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
      checkImported: React.PropTypes.func.isRequired,
      csrfToken: React.PropTypes.string.isRequired,
      description: React.PropTypes.string,
      name: React.PropTypes.string.isRequired,
      onBehaviorImport: React.PropTypes.func.isRequired
    },

    behaviorIsImported: function(behavior) {
      if (!behavior.config || !behavior.config.publishedId) {
        return false;
      }
      return this.props.checkImported(behavior.config.publishedId);
    },

    getBehaviors: function() {
      return this.props.behaviors || [];
    },

    render: function() {
      return (
        <div className="ptxl">
          <h3 className="border-bottom mtm pbm">
            <span>{this.props.name}</span>
            <span className="type-m type-regular type-weak"> &nbsp; – &nbsp; {this.props.description}</span>
          </h3>
          {this.getBehaviors().map(function(behavior, index) {
            return (
              <Behavior
                key={"behavior" + index}
                behaviorData={behavior}
                isImported={this.behaviorIsImported(behavior)}
                csrfToken={this.props.csrfToken}
                triggers={behavior.triggers}
                onBehaviorImport={this.props.onBehaviorImport}
              />
            );
          }, this)}
        </div>
      );
    }
  });
});
