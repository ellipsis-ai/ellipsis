import * as React from 'react';
import SettingsMenu from './settings_menu';

class SettingsPage extends React.Component {
    render() {
      const header = this.props.header;
      return (
        <div className="flex-row-cascade">
          <div className="bg-light">
            <div className="container container-wide pbm">
              {typeof header === "string" ? (
                <h3 className="mvn ptxxl type-weak display-ellipsis">
                  <span className="mrs">{header}</span>
                </h3>
              ) : header}
            </div>
          </div>
          <div className="flex-columns flex-row-expand">
            <div className="flex-column flex-column-left flex-rows container container-wide prn">
              <div className="columns flex-columns flex-row-expand">
                <div className="column column-one-quarter flex-column">
                  <SettingsMenu activePage={this.props.activePage} teamId={this.props.teamId} isAdmin={this.props.isAdmin}/>
                </div>
                <div className="column column-three-quarters flex-column bg-white ptxl pbxxxxl phxxxxl">

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
  header: React.PropTypes.node.isRequired,
  children: React.PropTypes.node.isRequired
};

export default SettingsPage;
