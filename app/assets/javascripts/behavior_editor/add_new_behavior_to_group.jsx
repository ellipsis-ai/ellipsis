define(function(require) {
  var React = require('react');

  return React.createClass({
    propTypes: {
      groupId: React.PropTypes.string.isRequired,
      teamId: React.PropTypes.string.isRequired
    },

    onAddNewBehavior: function() {
      window.location.href = jsRoutes.controllers.BehaviorEditorController.newForNormalBehavior(this.props.groupId, this.props.teamId).url;
    },

    render: function() {
      return (
        <div className="align-r ptxl">
          <button
            onClick={this.onAddNewBehavior}
            className="button-s">Add another action to this skill</button>
        </div>
      );
    }
  });

});

