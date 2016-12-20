define(function(require) {
  var React = require('react'),
    BehaviorGroupCard = require('./behavior_group_card'),
    BehaviorGroupInfoPanel = require('./behavior_group_info_panel'),
    Collapsible = require('../collapsible'),
    FixedFooter = require('../fixed_footer'),
    ModalScrim = require('../modal_scrim');

  return React.createClass({
    propTypes: {
      teamId: React.PropTypes.string.isRequired,
      installedBehaviorGroups: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
      behaviorGroups: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
      csrfToken: React.PropTypes.string.isRequired
    },

    getLocalId: function(group) {
      const installed = this.getInstalledBehaviorGroups().find(ea => {
        return ea.importedId === group.publishedId;
      });
      return installed ? installed.groupId : null;
    },

    getInstalledBehaviorGroups: function() {
      return this.state.installedBehaviorGroups || [];
    },

    getBehaviorGroups: function() {
      return this.state.behaviorGroups || [];
    },

    getInitialState: function() {
      return {
        installedBehaviorGroups: this.props.installedBehaviorGroups,
        behaviorGroups: this.props.behaviorGroups,
        selectedBehaviorGroup: null,
        revealMoreInfo: false,
        importingList: []
      };
    },

    onBehaviorGroupImport: function(groupToInstall) {
      this.setState({
        importingList: this.state.importingList.concat([groupToInstall])
      });
      var headers = new Headers();
      headers.append('x-requested-with', 'XMLHttpRequest');
      var body = new FormData();
      body.append('csrfToken', this.props.csrfToken);
      body.append('teamId', this.props.teamId);
      body.append('dataJson', JSON.stringify(groupToInstall));
      fetch(jsRoutes.controllers.BehaviorImportExportController.doImport().url, {
        credentials: 'same-origin',
        headers: headers,
        method: 'POST',
        body: body
      }).then((response) => response.json())
        .then((installedGroup) => {
          this.setState({
            importingList: this.state.importingList.filter((ea) => ea !== groupToInstall),
            installedBehaviorGroups: this.getInstalledBehaviorGroups().concat([installedGroup])
          });
        });
    },

    isImporting: function(group) {
      return this.state.importingList.some((ea) => ea === group);
    },

    getSelectedBehaviorGroup: function() {
      return this.state.selectedBehaviorGroup;
    },

    toggleInfoPanel: function(group) {
      var newState = {
        revealMoreInfo: !this.state.revealMoreInfo
      };
      if (group && group !== newState.selectedBehaviorGroup) {
        newState.selectedBehaviorGroup = group;
      }
      this.setState(newState);
    },

    render: function() {
      return (
        <div className="ptxxl">
          <div className="columns">
            {this.getBehaviorGroups().map((group, index) => (
              <div className="column column-one-third narrow-column-one-half mobile-column-full phl pbxxl mobile-phn" key={"group" + index}>
                <BehaviorGroupCard
                  name={group.name}
                  description={group.description}
                  icon={group.icon}
                  groupData={group}
                  localId={this.getLocalId(group)}
                  onBehaviorGroupImport={this.onBehaviorGroupImport}
                  onMoreInfoClick={this.toggleInfoPanel}
                  isImporting={this.isImporting(group)}
                />
              </div>
            ))}
          </div>

          <ModalScrim isActive={this.state.revealMoreInfo} onClick={this.toggleInfoPanel} />
          <FixedFooter className={this.state.revealMoreInfo ? "" : "display-none"}>
            <Collapsible revealWhen={this.state.revealMoreInfo}>
              <BehaviorGroupInfoPanel
                csrfToken={this.props.csrfToken}
                groupData={this.getSelectedBehaviorGroup()}
                onBehaviorGroupImport={this.onBehaviorGroupImport}
                onToggle={this.toggleInfoPanel}
              />
            </Collapsible>
          </FixedFooter>
        </div>
      );
    }
  });
});
