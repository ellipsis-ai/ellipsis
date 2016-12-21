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
      const installed = this.getAllInstalledBehaviorGroups().find(ea => ea.importedId === group.publishedId);
      return installed ? installed.groupId : null;
    },

    getAllInstalledBehaviorGroups: function() {
      return (this.props.installedBehaviorGroups || []).concat(this.getRecentlyInstalledBehaviorGroups());
    },

    getRecentlyInstalledBehaviorGroups: function() {
      return this.state.recentlyInstalledBehaviorGroups;
    },

    hasRecentlyInstalledBehaviorGroups: function() {
      return this.getRecentlyInstalledBehaviorGroups().length > 0;
    },

    getBehaviorGroups: function() {
      return this.state.behaviorGroups || [];
    },

    getInitialState: function() {
      return {
        recentlyInstalledBehaviorGroups: [],
        behaviorGroups: this.props.behaviorGroups,
        selectedBehaviorGroup: null,
        revealMoreInfo: false,
        activePanel: null,
        previousActivePanel: null,
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
            recentlyInstalledBehaviorGroups: this.getRecentlyInstalledBehaviorGroups().concat([installedGroup])
          });
        });
    },

    isImporting: function(group) {
      return this.state.importingList.some((ea) => ea === group);
    },

    getSelectedBehaviorGroup: function() {
      return this.state.selectedBehaviorGroup;
    },

    getActivePanel: function() {
      return this.state.activePanel;
    },

    getPreviousActivePanel: function() {
      return this.state.previousActivePanel;
    },

    activePanelIsNamed: function(name) {
      const panel = this.getActivePanel();
      return !!(panel && panel.name === name);
    },

    activePanelIsModal: function() {
      const panel = this.getActivePanel();
      return !!(panel && panel.isModal);
    },

    toggleActivePanel: function(name, beModal, optionalCallback) {
      var previousPanel = this.getPreviousActivePanel();
      var newPanel = this.activePanelIsNamed(name) ? previousPanel : { name: name, isModal: !!beModal };
      this.setState({
        activePanel: newPanel,
        previousActivePanel: this.getActivePanel()
      }, optionalCallback);
    },

    toggleInfoPanel: function(group) {
      if (group && group !== this.state.selectedBehaviorGroup) {
        this.setState({
          selectedBehaviorGroup: group
        });
      }
      this.toggleActivePanel('revealMoreInfo', true);
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

          <ModalScrim isActive={this.activePanelIsModal()} onClick={this.toggleInfoPanel} />
          <FixedFooter>
            <Collapsible revealWhen={this.activePanelIsNamed('revealMoreInfo')}>
              <BehaviorGroupInfoPanel
                groupData={this.getSelectedBehaviorGroup()}
                onBehaviorGroupImport={this.onBehaviorGroupImport}
                onToggle={this.toggleInfoPanel}
              />
            </Collapsible>

            <Collapsible revealWhen={this.hasRecentlyInstalledBehaviorGroups() && !this.getActivePanel()}>
              <div></div>
            </Collapsible>
          </FixedFooter>
        </div>
      );
    }
  });
});
