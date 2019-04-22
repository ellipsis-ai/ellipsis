import * as React from 'react';
import autobind from '../../lib/autobind';
import {PageRequiredProps} from "../../shared_ui/page";
import SVGCheckmark from "../../svg/checkmark";
import HelpButton from "../../help/help_button";
import Collapsible from "../../shared_ui/collapsible";
import HelpPanel from "../../help/panel";
import {SkillManifestDevelopmentStatus, SkillManifestItem} from "./loader";

type Props = PageRequiredProps & {
  csrfToken: string
  isAdmin: boolean
  teamId: string
  items: Array<SkillManifestItem>
}

class SkillManifest extends React.Component<Props> {
  constructor(props: Props) {
    super(props);
    autobind(this);
  }

  getMainColumnStyle(): React.CSSProperties {
    return {
      paddingBottom: this.props.footerHeight
    };
  }

  toggleStatusHelp(): void {
    this.props.onToggleActivePanel("statusHelp");
  }

  renderActive() {
    return (
      <div className="align-c">
        <div className="type-label border-top border-side border-blue bg-blue-lighter type-blue-faded phxs">Active</div>
      </div>
    );
  }

  renderInactive() {
    return (
      <div className="align-c">
        <div className="type-label border-top border-side bg-light type-weak phxs">Inactive</div>
      </div>
    );
  }

  renderProduction() {
    return (
      <div className="align-c">
        <div className="type-label border border-green bg-green-light type-green phxs">Production</div>
      </div>
    );
  }
  
  renderDevelopment() {
    return (
      <div className="align-c">
        <div className="type-label border border-pink-light bg-pink-light type-pink phxs">Development</div>
      </div>
    );
  }

  renderRequested() {
    return (
      <div className="align-c">
        <div className="type-label border border-black bg-almost-black type-white phxs">Requested</div>
      </div>
    );
  }

  renderDevelopmentStatus(status: SkillManifestDevelopmentStatus) {
    if (status === "Production") {
      return this.renderProduction();
    } else if (status === "Development") {
      return this.renderDevelopment();
    } else if (status === "Requested") {
      return this.renderRequested();
    } else {
      return null;
    }
  }

  renderItem(item: SkillManifestItem, index: number) {
    return (
      <tr key={item.id || `item${index}`}>
        <td>
          {item.id ? (
            <a className="type-bold" href={jsRoutes.controllers.BehaviorEditorController.edit(item.id).url}>{item.name}</a>
          ) : (
            <b>{item.name}</b>
          )}
        </td>
        <td>{item.editor}</td>
        <td>{item.description}</td>
        <td>
          {item.active ? this.renderActive() : this.renderInactive()}
          {this.renderDevelopmentStatus(item.developmentStatus)}
        </td>
        <td className="align-c">
          {item.managed ? (
            <span className="type-green display-inline-block height-l">
              <SVGCheckmark label="Managed" />
            </span>
          ) : (
            <span>—</span>
          )}
        </td>
        <td className="align-r">{item.lastUsed}</td>
      </tr>
    )
  }

  render() {
    return (
      <div className="flex-row-cascade">
        <div className="flex-columns flex-row-expand">
          <div className="flex-columns flex-row-expand">
            <div className="flex-column flex-column-left flex-rows container container-wide phn">
              <div className="columns flex-columns flex-row-expand mobile-flex-no-columns">
                <div className="column column-page-sidebar flex-column flex-column-left bg-lightest mobile-border-bottom prn">
                  <nav className="mvxxl phxl">
                    <ul className="list-nav">
                      <li><a href={jsRoutes.controllers.DashboardController.usage(this.props.isAdmin ? this.props.teamId : null).url}>Usage report</a></li>
                      <li className="list-nav-active-item">Skill manifest</li>
                    </ul>

                    <div className="type-s">
                      <div>13 active</div>
                      <div>22 managed</div>
                      <div>11 charged (active/managed)</div>
                    </div>

                  </nav>
                </div>
                <div
                  className="column column-page-main column-page-main-wide flex-column flex-column-main position-relative bg-white"
                  style={this.getMainColumnStyle()}
                >
                  <div className="phxl pvl">

                    <table className="border column-space type-s">
                      <thead>
                        <tr>
                          <th className="width-10">Skill name</th>
                          <th className="width-10">Contact</th>
                          <th>Description</th>
                          <th className="align-c">
                            <span className="mrm">Status</span>
                            <HelpButton onClick={this.toggleStatusHelp} toggled={this.props.activePanelName === "statusHelp"} />
                          </th>
                          <th className="align-c">Managed</th>
                          <th className="width-5 align-r">Last&nbsp;Used</th>
                        </tr>
                      </thead>
                      <tbody>
                        {this.props.items.map(this.renderItem)}
                      </tbody>
                    </table>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        {this.props.onRenderFooter(
          <Collapsible revealWhen={this.props.activePanelName === "statusHelp"}>
            <HelpPanel heading={"Skill status glossary"} onCollapseClick={this.props.onClearActivePanel}>
              <p className="type-m">Skills are packages of one or more related workflow actions.</p>

              <div><div className="display-inline-block border-bottom border-blue">{this.renderActive()}</div></div>
              <p>At least one workflow in the skill has been used in the past 30 days.</p>

              <div><div className="display-inline-block border-bottom">{this.renderInactive()}</div></div>
              <p>No workflows in the skill have been used in the past 30 days.</p>

              <div><div className="display-inline-block">{this.renderProduction()}</div></div>
              <p>The skill is deployed and available to users.</p>

              <div><div className="display-inline-block">{this.renderDevelopment()}</div></div>
              <p>The skill is in development, and only available to developers or test users.</p>

              <div><div className="display-inline-block">{this.renderRequested()}</div></div>
              <p>A new skill has been requested, but not yet approved for development.</p>

              <div>
                <div className="display-inline-block align-m mrs height-l">
                  <span className="type-green display-inline-block height-l">
                    <SVGCheckmark label="Managed" />
                  </span>
                </div>
                <span className="display-inline-block type-bold">Managed</span>
              </div>
              <p>The skill is managed by the Ellipsis team. Updates, upgrades, and modifications are done by the Ellipsis team. Users can make requests for updates in the shared Ellipsis support channel in chat.</p>

            </HelpPanel>
          </Collapsible>
        )}
      </div>
    );
  }
}

export default SkillManifest;
