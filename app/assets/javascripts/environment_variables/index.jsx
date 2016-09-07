define(function(require) {
  var React = require('react'),
    Collapsible = require('../collapsible'),
    HelpButton = require('../help/help_button'),
    HelpPanel = require('../help/panel'),
    SettingsMenu = require('../settings_menu'),
    Setter = require('./setter'),
    IfPresent = require('../if_present');

  return React.createClass({
    displayName: 'EnvironmentVariableList',
    propTypes: {
      csrfToken: React.PropTypes.string.isRequired,
      data: React.PropTypes.shape({
        teamId: React.PropTypes.string.isRequired,
        variables: React.PropTypes.arrayOf(React.PropTypes.shape({
          name: React.PropTypes.string.isRequired,
          isAlreadySavedWithName: React.PropTypes.bool.isRequired,
          isAlreadySavedWithValue: React.PropTypes.bool.isRequired
        })).isRequired
      })
    },

    getInitialState: function() {
      return {
        activePanel: null
      };
    },

    render: function() {
      return (
        <div>
          <div className="bg-light">
            <div className="container pbm">
              {this.renderHeader()}
            </div>
          </div>
          <div className="flex-container">
            <div className="container flex flex-center">
              <div className="columns">
                <div className="column column-one-quarter">
                  <SettingsMenu activePage="environmentVariables"/>
                </div>
                <div className="column column-three-quarters bg-white border-radius-bottom-left ptxl pbxxxxl phxxxxl">

                  {this.renderEnvVarList()}

                </div>
              </div>
            </div>
            <div className="flex flex-left"></div>
            <div className="flex flex-right bg-white"></div>
          </div>

          <footer ref="footer" className="position-fixed-bottom position-z-front border-top">
          </footer>
        </div>
      );
    },

    renderHeader: function() {
      return (
        <h3 className="mvn ptxxl type-weak display-ellipsis">
          <span className="mrs">Environment variables</span>
        </h3>
      );
    },

    renderEnvVarList: function() {
      return (
        <Setter
          onChangeVarName={function(){}}
          onSave={function(){}}
          vars={this.props.data.variables}
        />
      );
    }

  });
});
