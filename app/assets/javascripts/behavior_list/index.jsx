define(function(require) {
  var React = require('react'),
    BehaviorGroup = require('../models/behavior_group'),
    BehaviorGroupCard = require('./behavior_group_card'),
    BehaviorGroupInfoPanel = require('./behavior_group_info_panel'),
    Collapsible = require('../shared_ui/collapsible'),
    ConfirmActionPanel = require('../panels/confirm_action'),
    FixedFooter = require('../shared_ui/fixed_footer'),
    ModalScrim = require('../shared_ui/modal_scrim'),
    PageWithPanels = require('../shared_ui/page_with_panels');

  const ANIMATION_DURATION = 0.25;

  const BehaviorList = React.createClass({
    displayName: "BehaviorList",
    propTypes: Object.assign(PageWithPanels.requiredPropTypes(), {
      behaviorGroups: React.PropTypes.arrayOf(React.PropTypes.instanceOf(BehaviorGroup)).isRequired,
      csrfToken: React.PropTypes.string.isRequired
    }),

    getAnimationDuration: function() {
      return ANIMATION_DURATION;
    },

    getBehaviorGroups: function() {
      return this.props.behaviorGroups;
    },

    getInitialState: function() {
      return {
        selectedBehaviorGroup: null,
        selectedGroupIds: [],
        isSubmitting: false,
        footerHeight: 0
      };
    },

    resetFooterHeight: function() {
      var footerHeight = this.refs.footer.getHeight();
      if (this.state.footerHeight !== footerHeight) {
        this.setState({ footerHeight: footerHeight });
      }
    },

    getSelectedGroupIds: function() {
      return this.state.selectedGroupIds || [];
    },

    isGroupSelected: function(groupId) {
      return this.getSelectedGroupIds().indexOf(groupId) >= 0;
    },

    confirmDeleteBehaviorGroups: function() {
      this.toggleActivePanel('confirmDeleteBehaviorGroups', true);
    },

    confirmMergeBehaviorGroups: function() {
      this.toggleActivePanel('confirmMergeBehaviorGroups', true);
    },

    onGroupSelectionCheckboxChange: function(groupId, isChecked) {
      var newGroupIds = this.getSelectedGroupIds().slice();
      var index = newGroupIds.indexOf(groupId);
      if (isChecked) {
        if (index === -1) {
          newGroupIds.push(groupId);
        }
      } else {
        if (index >= 0) {
          newGroupIds.splice(index, 1);
        }
      }
      this.setState({
        selectedGroupIds: newGroupIds
      });
    },

    clearSelectedGroups: function() {
      this.setState({
        selectedGroupIds: []
      });
    },

    runSelectedBehaviorGroupsAction: function(url) {
      var data = {
        behaviorGroupIds: this.getSelectedGroupIds()
      };
      fetch(url, {
        credentials: 'same-origin',
        method: 'POST',
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json',
          'Csrf-Token': this.props.csrfToken
        },
        body: JSON.stringify(data)
      }).then(() => {
        window.location.reload();
      });
    },

    mergeBehaviorGroups: function() {
      this.setState({
        isSubmitting: true
      }, () => {
        var url = jsRoutes.controllers.ApplicationController.mergeBehaviorGroups().url;
        this.runSelectedBehaviorGroupsAction(url);
      });
    },

    deleteBehaviorGroups: function() {
      this.setState({
        isSubmitting: true
      }, () => {
        var url = jsRoutes.controllers.ApplicationController.deleteBehaviorGroups().url;
        this.runSelectedBehaviorGroupsAction(url);
      });
    },

    getActionsLabel: function(selectedCount) {
      if (selectedCount === 0) {
        return "No skills selected";
      } else if (selectedCount === 1) {
        return "1 skill selected";
      } else {
        return `${selectedCount} skills selected`;
      }
    },

    getLabelForDeleteAction: function(selectedCount) {
      if (selectedCount < 2) {
        return "Delete skill";
      } else {
        return `Delete skills`;
      }
    },

    getTextForDeleteBehaviorGroups: function(selectedCount) {
      if (selectedCount === 1) {
        return "Are you sure you want to delete this skill?";
      } else {
        return `Are you sure you want to delete these ${selectedCount} skills?`;
      }
    },

    getTextForMergeBehaviorGroups: function(selectedCount) {
      return `Are you sure you want to merge these ${selectedCount} skills?`;
    },

    getSelectedBehaviorGroup: function() {
      return this.state.selectedBehaviorGroup;
    },

    getSelectedBehaviorGroupId: function() {
      var group = this.getSelectedBehaviorGroup();
      return group ? group.id : null;
    },

    getActivePanelName: function() {
      return this.props.activePanelName;
    },

    clearActivePanel: function() {
      this.props.onClearActivePanel();
    },

    toggleActivePanel: function(panelName, beModal) {
      this.props.onToggleActivePanel(panelName, beModal);
    },

    toggleInfoPanel: function(group) {
      var previousSelectedGroup = this.state.selectedBehaviorGroup;
      var panelOpen = this.getActivePanelName() === 'moreInfo';

      if (panelOpen) {
        this.clearActivePanel();
      }

      if (group && group === previousSelectedGroup && !panelOpen) {
        this.toggleActivePanel('moreInfo');
      } else if (group && group !== previousSelectedGroup) {
        var openNewGroup = () => {
          this.setState({
            selectedBehaviorGroup: group
          }, () => {
            this.toggleActivePanel('moreInfo');
          });
        };
        if (panelOpen) {
          setTimeout(openNewGroup, this.getAnimationDuration() * 1000);
        } else {
          openNewGroup();
        }
      }
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
              localId={group.id}
              onMoreInfoClick={this.toggleInfoPanel}
              isImportable={false}
              onSelectChange={this.onGroupSelectionCheckboxChange}
              isSelected={this.isGroupSelected(group.id)}
            />
          </div>
        ));
      }
    },

    renderActions: function() {
      var selectedCount = this.getSelectedGroupIds().length;
      return (
        <div>
          <button type="button"
            className="button-primary mrs mbs"
            onClick={this.clearSelectedGroups}
          >
            Cancel
          </button>
          <button type="button"
            className="mrs mbs"
            onClick={this.confirmDeleteBehaviorGroups}
            disabled={selectedCount < 1}
          >
            {this.getLabelForDeleteAction(selectedCount)}
          </button>
          <button type="button"
            className="mrl mbs"
            onClick={this.confirmMergeBehaviorGroups}
            disabled={selectedCount < 2}
          >
            Merge skills
          </button>
          <div className="align-button mrs mbs type-italic type-weak">
            {this.getActionsLabel(selectedCount)}
          </div>
        </div>
      );
    },

    renderContent: function() {
      if (this.props.behaviorGroups.length > 0) {
        return (
          <div>
            <p className="mhl mbxl"><i><b>Tip:</b> mention Ellipsis in chat by starting a message with “…”</i></p>

            <div className="columns">
              {this.renderBehaviorGroups()}
            </div>
          </div>
        );
      } else {
        return (
          <p className="type-l pvxl">
          Ellipsis doesn’t know any skills yet. Try installing some of the ones
          published by Ellipsis, or create a new one yourself.
          </p>
        );
      }
    },

    render: function() {
      return (
        <div>
          <div style={{ paddingBottom: `${this.state.footerHeight}px` }}>
            <div className="bg-white container container-c ptxxl mobile-ptm phn">
              {this.renderContent()}
            </div>
          </div>

          <ModalScrim isActive={this.props.activePanelIsModal} onClick={this.clearActivePanel} />
          <FixedFooter ref="footer" className="bg-white">
            <Collapsible
              ref="moreInfo"
              revealWhen={this.getActivePanelName() === 'moreInfo'}
              animationDuration={this.getAnimationDuration()}
              onChange={this.resetFooterHeight}
            >
              <BehaviorGroupInfoPanel
                groupData={this.getSelectedBehaviorGroup()}
                onToggle={this.toggleInfoPanel}
                isImportable={false}
                localId={this.getSelectedBehaviorGroupId()}
              />
            </Collapsible>
            <Collapsible
              revealWhen={!this.getActivePanelName() && this.getSelectedGroupIds().length > 0}
              onChange={this.resetFooterHeight}
            >
              <div className="border-top">
                <div className="container container-c ptm">
                  {this.renderActions()}
                </div>
              </div>
            </Collapsible>
            <Collapsible ref="confirmDeleteBehaviorGroups"
              revealWhen={this.getActivePanelName() === 'confirmDeleteBehaviorGroups'}
              onChange={this.resetFooterHeight}
            >
              <ConfirmActionPanel
                confirmText="Delete"
                confirmingText="Deleting"
                onConfirmClick={this.deleteBehaviorGroups}
                onCancelClick={this.clearActivePanel}
                isConfirming={this.state.isSubmitting}
              >
                <p>{this.getTextForDeleteBehaviorGroups(this.getSelectedGroupIds().length)}</p>
              </ConfirmActionPanel>
            </Collapsible>
            <Collapsible ref="confirmMergeBehaviorGroups"
              revealWhen={this.getActivePanelName() === 'confirmMergeBehaviorGroups'}
              onChange={this.resetFooterHeight}
            >
              <ConfirmActionPanel
                confirmText="Merge"
                confirmingText="Merging"
                onConfirmClick={this.mergeBehaviorGroups}
                onCancelClick={this.clearActivePanel}
                isConfirming={this.state.isSubmitting}
              >
                <p>{this.getTextForMergeBehaviorGroups(this.getSelectedGroupIds().length)}</p>
              </ConfirmActionPanel>
            </Collapsible>
          </FixedFooter>
        </div>
      );
    }
  });

  return PageWithPanels.with(BehaviorList);
});
