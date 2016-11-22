define(function(require) {
  var React = require('react');

  return React.createClass({
    propTypes: {
      groupId: React.PropTypes.string.isRequired,
      teamId: React.PropTypes.string.isRequired
    },
    render: function() {
      return (
        <div className="column column-right align-r">
          <a
            href={jsRoutes.controllers.BehaviorEditorController.newForNormalBehavior(this.props.groupId, this.props.teamId).url}
            className="button">Add another action</a>
        </div>
      );
    }
  });

});

