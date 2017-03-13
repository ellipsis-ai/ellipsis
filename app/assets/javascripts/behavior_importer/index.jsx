define(function(require) {
  var React = require('react'),
    BehaviorGroup = require('../models/behavior_group'),
    BehaviorGroupCard = require('../behavior_list/behavior_group_card'),
    BehaviorGroupInfoPanel = require('../behavior_list/behavior_group_info_panel'),
    Collapsible = require('../shared_ui/collapsible'),
    FixedFooter = require('../shared_ui/fixed_footer'),
    InstalledBehaviorGroupsPanel = require('./installed_behavior_groups_panel'),
    ModalScrim = require('../shared_ui/modal_scrim'),
    PageWithPanels = require('../shared_ui/page_with_panels');

  var ANIMATION_DURATION = 0.25;

  const BehaviorImporter = React.createClass({
    propTypes: Object.assign(PageWithPanels.requiredPropTypes(), {
      teamId: React.PropTypes.string.isRequired,
      installedBehaviorGroups: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
      behaviorGroups: React.PropTypes.arrayOf(React.PropTypes.instanceOf(BehaviorGroup)).isRequired,
      csrfToken: React.PropTypes.string.isRequired,
      slackTeamId: React.PropTypes.string
    }),

    getLocalId: function(group) {
      if (group) {
        const installed = this.getAllInstalledBehaviorGroups().find((ea) => ea.exportId && ea.exportId === group.exportId);
        return installed ? installed.groupId : null;
      } else {
        return null;
      }
    },

    getAllInstalledBehaviorGroups: function() {
      return (this.props.installedBehaviorGroups || []).concat(this.state.recentlyInstalledBehaviorGroups);
    },

    getBehaviorGroupsJustInstalled: function() {
      return this.props.behaviorGroups.filter((group) => {
        return this.state.recentlyInstalledBehaviorGroups.some((recent) => recent.exportId === group.exportId);
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
          if (this.props.activePanelName !== 'afterInstall') {
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

    toggleInfoPanel: function(group) {
      if (group && group !== this.state.selectedBehaviorGroup) {
        this.setState({
          selectedBehaviorGroup: group
        });
      }
      this.props.onToggleActivePanel('moreInfo', true);
    },

    toggleAfterInstallPanel: function() {
      this.props.onToggleActivePanel('afterInstall');
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

    reload: function() {
      window.location.reload(true);
    },

    render: function() {
      return (
        <div className="ptxxl" style={{ paddingBottom: `${this.state.footerHeight}px` }}>
          <div className="columns">
            {this.renderBehaviorGroups()}
          </div>

          <ModalScrim isActive={this.props.activePanelIsModal} onClick={this.toggleInfoPanel} />
          <FixedFooter ref="footer">
            <Collapsible
              ref="moreInfo"
              revealWhen={this.props.activePanelName === 'moreInfo'}
              animationDuration={ANIMATION_DURATION}
            >
              <BehaviorGroupInfoPanel
                groupData={this.getSelectedBehaviorGroup()}
                onBehaviorGroupImport={this.onBehaviorGroupImport}
                onToggle={this.toggleInfoPanel}
                isImportable={true}
                localId={this.getLocalId(this.getSelectedBehaviorGroup())}
              />
            </Collapsible>

            <Collapsible
              ref="afterInstall"
              revealWhen={this.hasRecentlyInstalledBehaviorGroups() && this.props.activePanelName === 'afterInstall'}
              animationDuration={ANIMATION_DURATION}
            >
              <InstalledBehaviorGroupsPanel
                installedBehaviorGroups={this.getBehaviorGroupsJustInstalled()}
                onToggle={this.props.onClearActivePanel}
                slackTeamId={this.props.slackTeamId}
              />
            </Collapsible>
          </FixedFooter>
        </div>
      );
    },

    renderBehaviorGroups: function() {
      var groups = this.getBehaviorGroups();
      if (groups.length > 0) {
        return groups.map((group, index) => (
          <div className="column column-one-third narrow-column-one-half mobile-column-full phl pbxxl mobile-phn"
            key={"group" + index}>
            <BehaviorGroupCard
              name={group.name}
              description={group.description}
              icon={group.icon}
              groupData={group}
              localId={this.getLocalId(group)}
              onBehaviorGroupImport={this.onBehaviorGroupImport}
              onMoreInfoClick={this.toggleInfoPanel}
              isImporting={this.isImporting(group)}
              isImportable={true}
            />
          </div>
        ));
      } else {
        return (
          <div>
            <p>
              An error occurred while loading the list of skills. Please try again later.
            </p>

            <div className="mtl">
              <button type="button" onClick={this.reload}>Reload the page</button>
            </div>
          </div>
        );
      }
    }
  });

  return PageWithPanels.with(BehaviorImporter);
});
