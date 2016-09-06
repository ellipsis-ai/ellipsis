define(function(require) {
  var React = require('react'),
    Collapsible = require('../collapsible'),
    HelpButton = require('../help/help_button'),
    HelpPanel = require('../help/panel'),
    SettingsMenu = require('../settings_menu'),
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

                  <p>
                    <span>Use environment variables to hold secure information like passwords or access keys </span>
                    <span>that shouldn’t be visible to your team but may be used by multiple behaviors. </span>
                  </p>

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
        <div className="columns">
          <div className="column-group">
            {this.props.data.variables.map((envVar, index) => {
              return (
                <div className="column-row" key={`envVar${index}`}>
                  <div className="column column-one-quarter mobile-column-full border-top ptm pbs type-monospace display-ellipsis">
                    {envVar.name}
                  </div>
                  <div className="column column-three-quarters mobile-column-full border-top mobile-border-none ptm pbs mobile-ptn">
                    {IfPresent(envVar.isAlreadySavedWithValue, () => {
                      return (
                        <div>
                          <span>••••••••</span>
                          <button type="button" className="button-raw mlm">Reset</button>
                        </div>

                      );
                    }, () => {
                      return (
                        <i>No value set</i>
                      );
                    })}
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      );
    }

  });
});
