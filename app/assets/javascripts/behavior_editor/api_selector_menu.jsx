define(function (require) {
  var React = require('react'),
    DropdownMenu = require('./dropdown_menu');

  return React.createClass({
    propTypes: {
      openWhen: React.PropTypes.bool.isRequired,
      onAWSClick: React.PropTypes.func.isRequired,
      awsCheckedWhen: React.PropTypes.bool.isRequired,
      toggle: React.PropTypes.func.isRequired,
      allOAuth2Applications: React.PropTypes.arrayOf(React.PropTypes.shape({
        applicationId: React.PropTypes.string.isRequired,
        displayName: React.PropTypes.string.isRequired
      })).isRequired,
      requiredOAuth2ApiConfigs: React.PropTypes.arrayOf(React.PropTypes.shape({
        apiId: React.PropTypes.string.isRequired,
        recommendedScope: React.PropTypes.string,
        application: React.PropTypes.shape({
          applicationId: React.PropTypes.string.isRequired,
          displayName: React.PropTypes.string.isRequired
        })
      })).isRequired,
      onAddOAuth2Application: React.PropTypes.func.isRequired,
      onRemoveOAuth2Application: React.PropTypes.func.isRequired,
      onNewOAuth2Application: React.PropTypes.func.isRequired
    },

    getAPISelectorDropdownLabel: function() {
      var activeApiConfigs = this.props.requiredOAuth2ApiConfigs.filter((ea) => !!ea.application);
      var activeAPICount = activeApiConfigs.length;
      if (this.props.awsCheckedWhen) {
        activeAPICount++;
      }
      if (activeAPICount > 0) {
        return (
          <span>
          <span>Third-party APIs </span>
          <span className="type-bold">({activeAPICount} active)</span>
        </span>
        );
      } else {
        return "Add third-party APIs";
      }
    },

    getAPISelectorLabelForApp: function(app) {
      if (app.displayName.match(/github/i)) {
        return (
          <span>
          <img className="align-m mrs" src="/assets/images/logos/GitHub-Mark-64px.png" height="24" />
          <span>{app.displayName}</span>
        </span>
        );
      } else {
        return (
          <span>{app.displayName}</span>
        );
      }
    },

    isRequiredOAuth2Application: function(app) {
      var appIndex = this.props.requiredOAuth2ApiConfigs.findIndex(function(ea) {
        return ea.application && ea.application.applicationId === app.applicationId;
      });
      return appIndex >= 0;
    },

    toggleOAuth2Application: function(app) {
      if (this.isRequiredOAuth2Application(app)) {
        this.props.onRemoveOAuth2Application(app);
      } else {
        this.props.onAddOAuth2Application(app);
      }
    },

    addNewOAuth2Application: function() {
      this.props.onNewOAuth2Application();
    },

    render: function () {
      return (
        <DropdownMenu
          openWhen={this.props.openWhen}
          label={this.getAPISelectorDropdownLabel()}
          labelClassName="button-s"
          toggle={this.props.toggle}
        >
          <DropdownMenu.Item
            onClick={this.props.onAWSClick}
            checkedWhen={this.props.awsCheckedWhen}
            label={(<img src="/assets/images/logos/aws_logo_web_300px.png" height="32" />)}
          />
          {this.props.allOAuth2Applications.map((app, index) => {
            return (
              <DropdownMenu.Item
                key={"oauth2-app-" + index}
                checkedWhen={this.isRequiredOAuth2Application(app)}
                onClick={this.toggleOAuth2Application.bind(this, app)}
                label={this.getAPISelectorLabelForApp(app)}
              />
            );
          })}
          <DropdownMenu.Item
            onClick={this.addNewOAuth2Application}
            className="border-top"
            label="Add new API application…"
          />
        </DropdownMenu>
      );
    }
  });
});
