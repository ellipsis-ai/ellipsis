define(function(require) {
  var React = require('react'),
    BehaviorName = require('../behavior_list/behavior_name'),
    SVGInstall = require('../svg/install'),
    ifPresent = require('../if_present');

  return React.createClass({
    displayName: 'BehaviorGroupInfoPanel',
    propTypes: {
      groupData: React.PropTypes.object,
      onBehaviorGroupImport: React.PropTypes.func.isRequired,
      onToggle: React.PropTypes.func.isRequired
    },

    getBehaviors: function() {
      var behaviorVersions = this.props.groupData && this.props.groupData.behaviorVersions || [];
      return behaviorVersions.filter((version) => !version.isDataType());
    },

    onImport: function() {
      this.props.onBehaviorGroupImport(this.props.groupData);
      this.props.onToggle();
    },

    toggle: function() {
      this.props.onToggle();
    },

    render: function() {
      if (!this.props.groupData) {
        return null;
      }
      return (
        <div className="box-action phn">
          <div className="container container-c">
            <div className="columns">
              <div className="column column-page-sidebar">
                <h3 className="mtn">
                  {ifPresent(this.props.groupData.icon, (icon) => (
                    <span className="mrm">{icon}</span>
                  ))}
                  <span>{this.props.groupData.name}</span>
                </h3>

                <div className="type-s">
                  <a target="_blank" href={this.props.groupData.githubUrl}>
                    View source on Github
                  </a>
                </div>
              </div>
              <div className="column column-page-main">
                {ifPresent(this.props.groupData.description, (desc) => (
                  <p>{desc}</p>
                ))}

                {this.renderBehaviors()}

                <div className="mvxl">
                  <button type="button" className="button-primary mrs mbs" onClick={this.onImport}>
                    <span className="display-inline-block align-b mrm pbxs" style={{ width: 25, height: 18 }}><SVGInstall /></span>
                    <span className="display-inline-block align-b">Install</span>
                  </button>
                  <button type="button" className="mrs mbs" onClick={this.toggle}>Done</button>
                </div>
              </div>
            </div>
          </div>
        </div>
      );
    },

    renderBehaviors: function() {
      var behaviors = this.getBehaviors();
      var behaviorCount = behaviors.length;
      return (
        <div className="type-s mtl">
          <h5 className="mbxs">{behaviorCount === 1 ? "1 action" : `${behaviorCount} actions`}</h5>
          {behaviors.map((behavior, index) => (
            <div className="mbxs" key={`group-${this.props.groupData.publishedId}-behavior${index}`}>
              <BehaviorName
                version={behavior}
                disableLink={true}
                limitTriggers={true}
              />
            </div>
          ))}
        </div>
      );
    }
  });
});
