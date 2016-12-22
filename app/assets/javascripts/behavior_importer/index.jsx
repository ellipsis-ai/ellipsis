define(function(require) {
  var React = require('react'),
    BehaviorGroupCard = require('./behavior_group_card'),
    BehaviorGroupInfoPanel = require('./behavior_group_info_panel'),
    Collapsible = require('../collapsible'),
    FixedFooter = require('../fixed_footer'),
    InstalledBehaviorGroupsPanel = require('./installed_behavior_groups_panel'),
    ModalScrim = require('../modal_scrim');

  var ANIMATION_DURATION = 0.25;

  return React.createClass({
    propTypes: {
      teamId: React.PropTypes.string.isRequired,
      installedBehaviorGroups: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
      behaviorGroups: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
      csrfToken: React.PropTypes.string.isRequired,
      slackTeamId: React.PropTypes.string
    },

    getLocalId: function(group) {
      const installed = this.getAllInstalledBehaviorGroups().find((ea) => ea.importedId === group.publishedId);
      return installed ? installed.groupId : null;
    },

    getAllInstalledBehaviorGroups: function() {
      return (this.props.installedBehaviorGroups || []).concat(this.state.recentlyInstalledBehaviorGroups);
    },

    getBehaviorGroupsJustInstalled: function() {
      return this.props.behaviorGroups.filter((group) => {
        return this.state.recentlyInstalledBehaviorGroups.some((recent) => recent.importedId === group.publishedId);
      });
    },

    hasRecentlyInstalledBehaviorGroups: function() {
      return this.getBehaviorGroupsJustInstalled().length > 0;
    },

    getBehaviorGroups: function() {
      return this.state.behaviorGroups || [];
    },

    getInitialState: function() {
      return {
        recentlyInstalledBehaviorGroups: [],
        behaviorGroups: this.props.behaviorGroups,
        selectedBehaviorGroup: null,
        moreInfo: false,
        activePanel: null,
        previousActivePanel: null,
        importingList: [],
        footerHeight: 0
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
            recentlyInstalledBehaviorGroups: this.state.recentlyInstalledBehaviorGroups.concat([installedGroup])
          });
          if (!this.activePanelIsNamed('afterInstall')) {
            this.toggleAfterInstallPanel();
          }
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
      this.toggleActivePanel('moreInfo', true);
    },

    toggleAfterInstallPanel: function() {
      this.toggleActivePanel('afterInstall');
    },

    resetFooterHeight: function() {
      var footerHeight = this.refs.footer.getHeight();
      if (this.state.footerHeight !== footerHeight) {
        this.setState({ footerHeight: footerHeight });
      }
    },

    componentDidUpdate: function() {
      window.setTimeout(() => { this.resetFooterHeight(); }, ANIMATION_DURATION * 1000);
    },

    render: function() {
      return (
        <div className="ptxxl" style={{ paddingBottom: `${this.state.footerHeight}px` }}>
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
          <FixedFooter ref="footer">
            <Collapsible revealWhen={this.activePanelIsNamed('moreInfo')} animationDuration={ANIMATION_DURATION}>
              <BehaviorGroupInfoPanel
                groupData={this.getSelectedBehaviorGroup()}
                onBehaviorGroupImport={this.onBehaviorGroupImport}
                onToggle={this.toggleInfoPanel}
              />
            </Collapsible>

            <Collapsible
              revealWhen={this.hasRecentlyInstalledBehaviorGroups() && this.activePanelIsNamed('afterInstall')}
              animationDuration={ANIMATION_DURATION}
            >
              <InstalledBehaviorGroupsPanel
                installedBehaviorGroups={this.getBehaviorGroupsJustInstalled()}
                onToggle={this.toggleAfterInstallPanel}
                slackTeamId={this.props.slackTeamId}
              />
            </Collapsible>
          </FixedFooter>
        </div>
      );
    }
  });
});
