import * as React from 'react';
import Formatter from '../../javascripts/lib/formatter';
import SVGCheckmark from '../../javascripts/svg/checkmark';
import URLCreator from '../../javascripts/lib/url_creator';

const InstalledBehaviorGroupsPanel = React.createClass({
    displayName: 'InstalledBehaviorGroupsPanel',
    propTypes: {
      installedBehaviorGroups: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
      onToggle: React.PropTypes.func.isRequired,
      slackTeamId: React.PropTypes.string
    },

    getInstalledBehaviorGroups: function() {
      return this.props.installedBehaviorGroups;
    },

    getHeading: function() {
      var groups = this.getInstalledBehaviorGroups();
      var groupNames = Formatter.formatList(groups, (ea) => ea.name);
      if (groups.length === 1) {
        return (
          <span>Ellipsis has a learned new skill: <b>{groupNames}</b></span>
        );
      } else {
        return (
          <span>Ellipsis has learned {groups.length} new skills: <b>{groupNames}</b></span>
        );
      }
    },

    getIntro: function() {
      var groups = this.getInstalledBehaviorGroups();
      if (groups.length === 1) {
        return (
          <span>Use your bot’s new skill:</span>
        );
      } else if (groups.length > 1) {
        return (
          <span>Use your bot’s new skills:</span>
        );
      }
    },

    getFirstTriggerFor: function(group) {
      var triggerText = "";
      group.behaviorVersions.find((version) => {
        return version.triggers.some((trigger) => {
          if (!trigger.isRegex && /\bhelp\b/i.test(trigger.text)) {
            triggerText = trigger.displayText();
            return true;
          } else {
            return false;
          }
        });
      });
      if (!triggerText) {
        group.behaviorVersions.find((version) => {
          return version.triggers.find((trigger) => {
            if (trigger.text) {
              triggerText = trigger.displayText();
              return true;
            } else {
              return false;
            }
          });
        });
      }
      return triggerText;
    },

    getBehaviorHelpInstructions: function() {
      var groups = this.getInstalledBehaviorGroups();
      var firstTrigger = groups[0] && this.getFirstTriggerFor(groups[0]);
      if (groups.length === 1 && firstTrigger) {
        return (
          <li>
            <span>In your channel, type <span className="box-chat-example">{firstTrigger}</span> </span>
            <span>to try out the {groups[0].name} skill, or </span>
            <span className="box-chat-example">...help</span>
            <span> to see everything Ellipsis can do so far.</span>
          </li>
        );
      } else {
        return (
          <li>
            <span>In your channel, type <span className="box-chat-example">...help</span> to see everything Ellipsis can do so far.</span>
          </li>
        );
      }
    },

    getSlackUrl: function() {
      return URLCreator.forSlack(this.props.slackTeamId);
    },

    getEditUrlFor: function(group) {
      return jsRoutes.controllers.BehaviorEditorController.edit(group.id).url;
    },

    onClickDone: function() {
      this.props.onToggle();
    },

    render: function() {
      const groups = this.getInstalledBehaviorGroups();
      const numGroups = groups.length;
      if (numGroups === 0) {
        return null;
      }
      return (
        <div className="bg-green-lightest border-emphasis-top border-green">
          <div className="bg-green-light">
            <div className="container container-c pvm">
              <span className="type-green display-inline-block mrs align-b" style={{ height: "24px" }}>
                <SVGCheckmark />
              </span>
              {this.getHeading()}
            </div>
          </div>

          <div className="container container-c pvxl">
            <div className="mbxl">{this.getIntro()}</div>

            <ul className="list-space-xl">
              <li>
                <span>Type <span className="box-chat-example">/invite @ellipsis</span> to add your </span>
                <span>bot to any channel.</span>
              </li>
              {this.getBehaviorHelpInstructions()}
            </ul>

            <div className="mtxl">
              <button type="button" className="mbs mrs" onClick={this.onClickDone}>Done</button>

              <a className="button mbs mrs button-primary" href={this.getSlackUrl()}>Open your Slack team</a>

              {groups.map((group, index) => (
                <a className="button mbs mrs" href={this.getEditUrlFor(group)} key={`group${index}`}>Edit {group.name}</a>
              ))}
            </div>

          </div>
        </div>
      );
    }
});

export default InstalledBehaviorGroupsPanel;
