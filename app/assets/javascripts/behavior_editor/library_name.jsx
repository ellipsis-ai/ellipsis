define(function(require) {
  var React = require('react'),
    LibraryVersion = require('../models/library_version');

  return React.createClass({
    displayName: 'LibraryName',
    propTypes: {
      version: React.PropTypes.instanceOf(LibraryVersion).isRequired,
      disableLink: React.PropTypes.bool,
      omitDescription: React.PropTypes.bool,
      labelDataType: React.PropTypes.bool,
      onClick: React.PropTypes.func,
      isImportable: React.PropTypes.bool,
      className: React.PropTypes.string,
      highlightText: React.PropTypes.string
    },

    onLinkClick: function(event) {
      if (this.props.onClick) {
        this.props.onClick(this.props.version.groupId, this.props.version.libraryId);
        event.preventDefault();
      }
    },

    render: function() {
      if (this.props.disableLink) {
        return (
          <div className={this.props.className || ""}>
            <div className="display-limit-width display-ellipsis">
              {this.props.version.name || "New library"}
            </div>
          </div>
        );
      } else {
        return (
          <div>
            <a
              href={jsRoutes.controllers.BehaviorEditorController.edit(this.props.version.groupId, this.props.version.libraryId).url}
              onClick={this.onLinkClick}
              className={"link-block " + (this.props.className || "")}>
              {this.props.version.name || "New library"}
            </a>
          </div>
        );
      }
    }
  });
});
