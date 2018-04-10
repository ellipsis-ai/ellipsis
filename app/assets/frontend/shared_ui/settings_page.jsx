import * as React from 'react';
import SettingsMenu from './settings_menu';

class SettingsPage extends React.Component {
    render() {
      return (
        <div className="flex-row-cascade">
          <div className="flex-columns flex-row-expand">
            <div className="flex-column flex-column-left flex-rows container container-wide width-full prn">
              <div className="columns flex-columns flex-row-expand">
                <div className="column column-one-quarter flex-column">
                  <SettingsMenu activePage={this.props.activePage} teamId={this.props.teamId} isAdmin={this.props.isAdmin}/>
                </div>
                <div className="column column-three-quarters flex-column bg-white ptxxl pbxxxxl phxxxxl">

                  {this.props.children}

                </div>
              </div>
            </div>
            <div className="flex-column flex-column-right bg-white" />
          </div>
        </div>
      );
    }
}

SettingsPage.propTypes = {
  teamId: React.PropTypes.string.isRequired,
  isAdmin: React.PropTypes.bool.isRequired,
  activePage: React.PropTypes.string,
  children: React.PropTypes.node.isRequired
};

export default SettingsPage;
