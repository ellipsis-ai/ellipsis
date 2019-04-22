import * as React from 'react';
import autobind from '../../lib/autobind';
import {PageRequiredProps} from "../../shared_ui/page";
import SVGCheckmark from "../../svg/checkmark";

type Props = PageRequiredProps & {
  csrfToken: string
  isAdmin: boolean
  teamId: string
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

  renderManaged() {
    return (
      <td className="align-c">
        <span className="type-green display-inline-block height-l">
          <SVGCheckmark label="Managed" />
        </span>
      </td>
    );
  }

  renderUnmanaged() {
    return (
      <td className="align-c">
        <span>—</span>
      </td>
    );
  }
  
  renderEditLinkFor(skillName: string, groupId: string) {
    return (
      <td>
        <a className="type-bold" href={jsRoutes.controllers.BehaviorEditorController.edit(groupId).url}>{skillName}</a>
      </td>
    );
  }

  renderLastUsed(date: string) {
    return (
      <td className="align-r">{date}</td>
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
                          <th className="align-c">Status</th>
                          <th className="align-c">Managed</th>
                          <th className="width-5 align-r">Last&nbsp;Used</th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr>
                          {this.renderEditLinkFor("Food Safety", "E5--ZnuhRrqjgEapQIhimg")}
                          <td>Isabel Chamberlain</td>
                          <td>Report and track food safety incidents</td>
                          <td>
                            {this.renderInactive()}
                            {this.renderProduction()}
                          </td>
                          {this.renderManaged()}
                          {this.renderLastUsed("Oct 2018 – Dec 2018")}
                        </tr>
                        <tr>
                          {this.renderEditLinkFor("Production Reports", "z4N1ITXRRImhInqti2pwUQ")}
                          <td>Emy Kelty</td>
                          <td>Collect and post production reports on schedule</td>
                          <td>
                            {this.renderInactive()}
                            {this.renderProduction()}
                          </td>
                          {this.renderManaged()}
                          {this.renderLastUsed("May 2018 – Feb 2019")}
                        </tr>
                        <tr>
                          {this.renderEditLinkFor("Seedling Germination Checklist & Reminder", "IxofxQGJTZ6Z8tNu6EefWw")}
                          <td>Jessica Kowalski</td>
                          <td>Checklist for process in germination, results stored in Google Sheets and a reminder set to unload germination chambers.</td>
                          <td>
                            {this.renderInactive()}
                            {this.renderProduction()}
                          </td>
                          {this.renderManaged()}
                          {this.renderLastUsed("Sept 2018 – Dec 2018")}
                        </tr>
                        <tr>
                          {this.renderEditLinkFor("Sensory QC Pre-check", "a2GyCpj9QCSqQy94IOlgtQ")}
                          <td>Molly Kreykes</td>
                          <td>Collect results of sensory testing for in-progress crops.</td>
                          <td>
                            {this.renderInactive()}
                            {this.renderProduction()}
                          </td>
                          {this.renderManaged()}
                          {this.renderLastUsed("May 2018 – Sept 2018")}
                        </tr>
                        <tr>
                          {this.renderEditLinkFor("Standup", "6cf3NCiKQcGUAYx7ogvdsA")}
                          <td>Perry Skorcz</td>
                          <td>Run standup</td>
                          <td>
                            {this.renderInactive()}
                            {this.renderProduction()}
                          </td>
                          {this.renderManaged()}
                          {this.renderLastUsed("Oct 2017 – Dec 2018")}
                        </tr>
                        <tr>
                          {this.renderEditLinkFor("SSF Root Cause Report", "NCrWrrSIT4m4RXvYxBx4gA")}
                          <td>Gabriella Carne</td>
                          <td>Create a failure root cause report</td>
                          <td>
                            {this.renderInactive()}
                            {this.renderProduction()}
                          </td>
                          {this.renderManaged()}
                          {this.renderLastUsed("Apr 2019")}
                        </tr>
                        <tr>
                          {this.renderEditLinkFor("SSF Seedling Systems Checklist", "SsbH1p6DR9WuHi5SPJewbg")}
                          <td>Jessica Kowalski</td>
                          <td>Check list for the seedling system</td>
                          <td>
                            {this.renderInactive()}
                            {this.renderProduction()}
                          </td>
                          {this.renderManaged()}
                          {this.renderLastUsed("Apr 2019")}
                        </tr>
                        <tr>
                          {this.renderEditLinkFor("SSF Systems Tracking", "diUzAcYbQI2qJrbbhnnWjw")}
                          <td>Teryl Chapel</td>
                          <td>Daily tracking of downtime across all Plenty systems in SSF</td>
                          <td>
                            {this.renderInactive()}
                            {this.renderProduction()}
                          </td>
                          {this.renderManaged()}
                          {this.renderLastUsed("Apr 2019")}
                        </tr>
                        <tr>
                          {this.renderEditLinkFor("Accidents", "eEbZigZ3QBe75pgalHF2sQ")}
                          <td>Yashira Frederick</td>
                          <td>In case of an accident, tell an employee where is the closes urgent care facility</td>
                          <td>
                            {this.renderActive()}
                            {this.renderProduction()}
                          </td>
                          {this.renderManaged()}
                          {this.renderLastUsed("Apr 2019")}
                        </tr>
                        <tr>
                          {this.renderEditLinkFor("CEO Briefing", "EkWR_K3_TnuQHVHIIpuxbg")}
                          <td>Jennie Chen</td>
                          <td>Collect and track items for the CEO briefing agenda.</td>
                          <td>
                            {this.renderActive()}
                            {this.renderProduction()}
                          </td>
                          {this.renderManaged()}
                          {this.renderLastUsed("Apr 2019")}
                        </tr>
                        <tr>
                          {this.renderEditLinkFor("Change Management", "PiapjCvVQFKc-Z71QBgl3w")}
                          <td>Gabriella Carne</td>
                          <td>Track farm ops change requests and approval</td>
                          <td>
                            {this.renderActive()}
                            {this.renderProduction()}
                          </td>
                          {this.renderManaged()}
                          {this.renderLastUsed("Apr 2019")}
                        </tr>
                        <tr>
                          {this.renderEditLinkFor("Farm Standup", "d8TFaKU7RHaOzC665hPpzA")}
                          <td>Jessica Kowalski</td>
                          <td>Run customized standup for the Farm team</td>
                          <td>
                            {this.renderActive()}
                            {this.renderProduction()}
                          </td>
                          {this.renderManaged()}
                          {this.renderLastUsed("Apr 2019")}
                        </tr>
                        <tr>
                          {this.renderEditLinkFor("Farm Training", "3xD5y4YnRPuvYJ9VEz5K2w")}
                          <td>Jessica Kowalski</td>
                          <td>Track expired training sessions from a Google sheet and send notifications in Slack reminding people to redo training sessions.</td>
                          <td>
                            {this.renderActive()}
                            {this.renderProduction()}
                          </td>
                          {this.renderManaged()}
                          {this.renderLastUsed("Apr 2019")}
                        </tr>
                        <tr>
                          {this.renderEditLinkFor("Give Kudos", "KRAcw-NNSqi9_l09KUVrTw")}
                          <td>Chris Michael</td>
                          <td>Recognize and celebrate co-workers</td>
                          <td>
                            {this.renderActive()}
                            {this.renderProduction()}
                          </td>
                          {this.renderManaged()}
                          {this.renderLastUsed("Apr 2019")}
                        </tr>
                        <tr>
                          {this.renderEditLinkFor("Growers Journal", "OY-nLppwQ1-xyBrnCEJxTQ")}
                          <td>Gabriella Carne</td>
                          <td>Daily journal of farm issues observed by growers</td>
                          <td>
                            {this.renderActive()}
                            {this.renderProduction()}
                          </td>
                          {this.renderManaged()}
                          {this.renderLastUsed("Apr 2019")}
                        </tr>
                        <tr>
                          {this.renderEditLinkFor("My Calendar", "LIO4h-8DQRaj8EDNTPc5hQ")}
                          <td>Jessica Kowalski</td>
                          <td>Report to the Farm team all upcoming facilities on-call schedules, weekend shifts, and PTO events </td>
                          <td>
                            {this.renderActive()}
                            {this.renderProduction()}
                          </td>
                          {this.renderManaged()}
                          {this.renderLastUsed("Apr 2019")}
                        </tr>
                        <tr>
                          {this.renderEditLinkFor("Sensory Results Checklist", "Rd2aHMmdQum-OoL9hvgbwg")}
                          <td>Molly Kreykes</td>
                          <td>Collect results of sensory testing before product release.</td>
                          <td>
                            {this.renderActive()}
                            {this.renderProduction()}
                          </td>
                          {this.renderManaged()}
                          {this.renderLastUsed("Apr 2019")}
                        </tr>
                        <tr>
                          {this.renderEditLinkFor("Systems Checklists", "jew0cCeGQGu3O3BcF1MYwA")}
                          <td>Jessica Kowalski</td>
                          <td>Ensure that the correct process is followed in production and research grow rooms and other facilities.</td>
                          <td>
                            {this.renderActive()}
                            {this.renderProduction()}
                          </td>
                          {this.renderManaged()}
                          {this.renderLastUsed("Apr 2019")}
                        </tr>
                        <tr>
                          {this.renderEditLinkFor("Work Requests", "xWwqXeF6RSeiY_2xPHEoyQ")}
                          <td>Gantt Charping</td>
                          <td>Support for creating work requests. These are saved as fiix.com tasks</td>
                          <td>
                            {this.renderActive()}
                            {this.renderProduction()}
                          </td>
                          {this.renderManaged()}
                          {this.renderLastUsed("Apr 2019")}
                        </tr>
                        <tr>
                          {this.renderEditLinkFor("Handbook", "UIdytfgFR2WK9CKbAu2aew")}
                          <td>Emy Kelty</td>
                          <td>Simple Q&A</td>
                          <td>
                            {this.renderActive()}
                            {this.renderProduction()}
                          </td>
                          {this.renderUnmanaged()}
                          {this.renderLastUsed("Apr 2019")}
                        </tr>
                        <tr>
                          {this.renderEditLinkFor("Empathic Bot", "cmt3ii2lS2qwt1Nc_wekLg")}
                          <td>Ellipsis</td>
                          <td>Greet users</td>
                          <td>
                            {this.renderActive()}
                            {this.renderProduction()}
                          </td>
                          {this.renderUnmanaged()}
                          {this.renderLastUsed("Apr 2019")}
                        </tr>
                        <tr>
                          {this.renderEditLinkFor("SF OM Requests", "82hUKRbET6CdRSViSeqeeQ")}
                          <td>Amanda Cabrera</td>
                          <td>Quickly add office management requests made in Slack and store them in a Google Sheet. Allow marking them as complete via Slack to make tracking easier.</td>
                          <td>
                            {this.renderInactive()}
                            {this.renderDevelopment()}
                          </td>
                          {this.renderManaged()}
                          {this.renderLastUsed("Apr 2019")}
                        </tr>
                        <tr>
                          {this.renderEditLinkFor("Work Request Scheduling", "IxofxQGJTZ6Z8tNu6EefWw")}
                          <td>Jessica Kowalski</td>
                          <td>Collect work request information for scheduling and planning</td>
                          <td>
                            {this.renderInactive()}
                            {this.renderDevelopment()}
                          </td>
                          {this.renderManaged()}
                          {this.renderLastUsed("Apr 2019")}
                        </tr>
                        <tr>
                          <td><b>Donation Tracker</b></td>
                          <td>Jessica Kowalski</td>
                          <td>Report how much produce has been donated each week </td>
                          <td>
                            {this.renderInactive()}
                            {this.renderRequested()}
                          </td>
                          {this.renderManaged()}
                          {this.renderLastUsed("—")}
                        </tr>
                      </tbody>
                    </table>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        {this.props.onRenderFooter()}
      </div>
    );
  }
}

export default SkillManifest;
