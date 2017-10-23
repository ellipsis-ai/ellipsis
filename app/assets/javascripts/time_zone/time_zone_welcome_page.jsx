define(function(require) {
  const React = require('react');

  class TeamTimeZoneWelcomePage extends React.Component {
    render() {
      return (
        <div className="bg-white border-bottom border-bottom-thick pvxl">
          <div className="container container-c container-narrow">
            <h2>Welcome to Ellipsis!</h2>

            <h3>Set your team’s time zone</h3>
            <p>
              Before you get started, pick a default time zone for your team.
            </p>

            <p>
              This will be used when Ellipsis displays dates and times to a group, or whenever a time of day mentioned
              isn’t otherwise obvious.
            </p>

            {this.props.children}
          </div>
        </div>
      );
    }
  }

  TeamTimeZoneWelcomePage.propTypes = {
    children: React.PropTypes.node.isRequired
  };

  return TeamTimeZoneWelcomePage;
});
