define(function (require) {
  var React = require('react');

  return React.createClass({
    propTypes: {
      activePage: React.PropTypes.string
    },

    activeClassWhenPageName: function(pageName) {
      return this.props.activePage === pageName ? "list-nav-active-item" : "";
    },

    render: function () {
      return (
        <nav className="mvxxl">
          <ul className="list-nav">
            <li className={this.activeClassWhenPageName("oauthApplications")}>
              <a href={jsRoutes.controllers.ApplicationController.newOAuth2Application().url}>API applications</a>
            </li>
          </ul>
        </nav>
      );
    }
  });
});
