import * as React from 'react';

const ListHeading = React.createClass({
    propTypes: {
      includeTeachButton: React.PropTypes.bool,
      children: React.PropTypes.node.isRequired,
      teamId: React.PropTypes.string.isRequired
    },

    render: function() {
      return (
        <div className="columns columns-elastic mobile-columns-float">
          <div className="column column-expand align-m">
            <h3 className="type-blue-faded mvn mhl mobile-mbm">{this.props.children}</h3>
          </div>
          {this.props.includeTeachButton ? (
              <div className="column column-shrink align-m phl mobile-pbl">
                <a href={jsRoutes.controllers.BehaviorEditorController.newGroup(this.props.teamId).url}
                  className="button button-shrink">
                  Teach Ellipsis something new…
                </a>
              </div>
            ) : null}
        </div>
      );
    }
});

export default ListHeading;
