define(function(require) {
  var React = require('react'),
    BehaviorImporterBehavior = require('./behavior_importer_behavior');

  return React.createClass({
    propTypes: {
      behaviors: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
      checkInstalled: React.PropTypes.func.isRequired,
      csrfToken: React.PropTypes.string.isRequired,
      description: React.PropTypes.string,
      name: React.PropTypes.string.isRequired
    },

    behaviorIsInstalled: function(behavior) {
      if (!behavior.config || !behavior.config.publishedId) {
        return false;
      }
      return this.props.checkInstalled(behavior.config.publishedId);
    },

    getBehaviors: function() {
      return this.props.behaviors || [];
    },

    render: function() {
      return (
        <div className="pvxl">
          <h3 className="border-bottom pbm">
            <span>{this.props.name}</span>
            <span className="type-m type-regular type-weak"> &nbsp; – &nbsp; {this.props.description}</span>
          </h3>
          {this.getBehaviors().map(function(behavior, index) {
            return (
              <BehaviorImporterBehavior
                key={"behavior" + index}
                behaviorData={behavior}
                isInstalled={this.behaviorIsInstalled(behavior)}
                csrfToken={this.props.csrfToken}
                triggers={behavior.triggers}
              />
            );
          }, this)}
        </div>
      );
    }
  });
});
