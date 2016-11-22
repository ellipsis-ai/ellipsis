define(function(require) {
  var React = require('react'),
    AddNewBehaviorToGroup = require('./add_new_behavior_to_group'),
    SVGHamburger = require('../svg/hamburger');

  return React.createClass({
    propTypes: {
      behaviorCount: React.PropTypes.number.isRequired,
      groupId: React.PropTypes.string.isRequired,
      teamId: React.PropTypes.string.isRequired
    },

    render: function() {
      if (this.props.behaviorCount > 1) {
        return (
          <div className="align-r ptxl">
            <button type="button" className="button-tab">
              <span>{this.props.behaviorCount} actions in this skill</span>
              <span className="display-inline-block align-b mlm" style={{height: "24px"}}>
                  <SVGHamburger />
                </span>
            </button>
          </div>
        );
      } else {
        return (
          <AddNewBehaviorToGroup
            key="add-new-behavior-to-group"
            groupId={this.props.groupId}
            teamId={this.props.teamId}
          />
        );
      }
    }
  });
});
