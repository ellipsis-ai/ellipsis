requirejs(['common'], function() {
  requirejs(
    ['core-js', 'whatwg-fetch', 'react', 'react-dom', './regional_settings/index',
      'config/regionalsettings/index', './shared_ui/page', './lib/autobind'],
    function(Core, Fetch, React, ReactDOM, RegionalSettings,
             RegionalSettingsConfiguration, Page, autobind) {

      class RegionalSettingsLoader extends React.Component {
        constructor(props) {
          super(props);
          autobind(this);
        }

        render() {
          return (
            <Page csrfToken={this.props.csrfToken}>
              <RegionalSettings csrfToken={this.props.csrfToken} />
            </Page>
          );
        }
      }

      RegionalSettingsLoader.propTypes = {
        containerId: React.PropTypes.string.isRequired,
        csrfToken: React.PropTypes.string.isRequired
      };

      ReactDOM.render(
        React.createElement(RegionalSettingsLoader, RegionalSettingsConfiguration),
        document.getElementById(RegionalSettingsConfiguration.containerId)
      );
    }
  );
});
