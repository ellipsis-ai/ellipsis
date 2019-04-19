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

  render() {
    return (
      <div className="flex-row-cascade">
        <div className="flex-columns flex-row-expand">
          <div className="flex-columns flex-row-expand">
            <div className="flex-column flex-column-left flex-rows container container-wide phn">
              <div className="columns flex-columns flex-row-expand mobile-flex-no-columns">
                <div className="column column-page-sidebar flex-column flex-column-left bg-lightest mobile-border-bottom prn">
                  <nav className="mvxxl plxl">
                    <ul className="list-nav">
                      <li><a href={jsRoutes.controllers.DashboardController.usage(this.props.isAdmin ? this.props.teamId : null).url}>Usage report</a></li>
                      <li className="list-nav-active-item">Skill manifest</li>
                    </ul>
                  </nav>
                </div>
                <div
                  className="column column-page-main column-page-main-wide flex-column flex-column-main position-relative bg-white"
                  style={this.getMainColumnStyle()}
                >
                  <div className="paxl">
                    <table className="border column-space type-s">
                      <thead>
                        <tr>
                          <th className="width-10">Skill name</th>
                          <th className="width-10">Contact</th>
                          <th>Description</th>
                          <th>Activity</th>
                          <th>Release Status</th>
                          <th>Managed</th>
                          <th>Last&nbsp;Used</th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr>
                          <td><a href={jsRoutes.controllers.BehaviorEditorController.edit("E5--ZnuhRrqjgEapQIhimg").url}>Food Safety</a></td>
                          <td>
                            <div className="">Isabel Chamberlain</div>
                          </td>
                          <td>
                            <div className="">Report and track food safety incidents</div></td>
                          <td>
                            <span className="type-label bg-yellow type-black phxs">Inactive</span>
                          </td>
                          <td>
                            <span className="type-label bg-green type-white phxs">Production</span>
                          </td>
                          <td>
                            <span className="type-green display-inline-block height-l mtxs">
                              <SVGCheckmark label="Managed" />
                            </span>
                          </td>
                          <td className="">Oct&nbsp;2018 -&nbsp;Dec&nbsp;2018</td>
                        </tr>
                        <tr>
                          <td>Production Reports</td>
                          <td>Emy Kelty</td>
                          <td>Collect and post production reports on schedule</td>
                          <td>Inactive</td>
                          <td>Production</td>
                          <td>Yes</td>
                          <td>May 2018 - Feb 2019</td>
                        </tr>
                        <tr>
                          <td>Seedling Germination Checklist & Reminder</td>
                          <td>Jessica Kowalski</td>
                          <td>Checklist for process in germination, results stored in Google Sheets and a reminder set to unload germination chambers.</td>
                          <td>Inactive</td>
                          <td>Production</td>
                          <td>Yes</td>
                          <td>Sept 2018 - Dec 2018</td>
                        </tr>
                        <tr>
                          <td>Sensory QC Pre-check</td>
                          <td>Molly Kreykes</td>
                          <td>Collect results of sensory testing for in-progress crops.</td>
                          <td>Inactive</td>
                          <td>Production</td>
                          <td>Yes</td>
                          <td>May 2018 - Sept 2018</td>
                        </tr>
                        <tr>
                          <td>SSF Root Cause Report</td>
                          <td>Gabriella Carne</td>
                          <td>Create a failure root cause report</td>
                          <td>Inactive</td>
                          <td>Production</td>
                          <td>Yes</td>
                          <td>Apr-19</td>
                        </tr>
                        <tr>
                          <td>SSF Seedling Systems Checklist</td>
                          <td>Jessica Kowalski</td>
                          <td>Check list for the seedling system</td>
                          <td>Inactive</td>
                          <td>Production</td>
                          <td>Yes</td>
                          <td>Apr-19</td>
                        </tr>
                        <tr>
                          <td>SSF Systems Tracking </td>
                          <td>Teryl Chapel</td>
                          <td>Daily tracking of downtime across all Plenty systems in SSF</td>
                          <td>Inactive</td>
                          <td>Production</td>
                          <td>Yes</td>
                          <td>Apr-19</td>
                        </tr>
                        <tr>
                          <td>Accidents</td>
                          <td>Yashira Frederick</td>
                          <td>In case of an accident, tell an employee where is the closes urgent care facility</td>
                          <td>Active</td>
                          <td>Production</td>
                          <td>Yes</td>
                          <td>Apr-19</td>
                        </tr>
                        <tr>
                          <td>CEO Briefing</td>
                          <td>Jennie Chen</td>
                          <td>Collect and track items for the CEO briefing agenda.</td>
                          <td>Active</td>
                          <td>Production</td>
                          <td>Yes</td>
                          <td>Apr-19</td>
                        </tr>
                        <tr>
                          <td>Change Management </td>
                          <td>Gabriella Carne</td>
                          <td>Track farm ops change requests and approval</td>
                          <td>Active</td>
                          <td>Production</td>
                          <td>Yes</td>
                          <td>Apr-19</td>
                        </tr>
                        <tr>
                          <td>Standup</td>
                          <td>Perry Skorcz</td>
                          <td>Run standup</td>
                          <td>Inactive</td>
                          <td>Production</td>
                          <td>Yes</td>
                          <td>Oct 2017 - Dec 2018</td>
                        </tr>
                        <tr>
                          <td>Farm Standup</td>
                          <td>Jessica Kowalski</td>
                          <td>Run customized standup for the Farm team</td>
                          <td>Active</td>
                          <td>Production</td>
                          <td>Yes</td>
                          <td>Apr-19</td>
                        </tr>
                        <tr>
                          <td>Give Kudos</td>
                          <td>Chris Michael</td>
                          <td>Recognize and celebrate co-workers</td>
                          <td>Active</td>
                          <td>Production</td>
                          <td>Yes</td>
                          <td>Apr-19</td>
                        </tr>
                        <tr>
                          <td>Growers Journal </td>
                          <td>Gabriella Carne</td>
                          <td>Daily journal of farm issues observed by growers</td>
                          <td>Active</td>
                          <td>Production</td>
                          <td>Yes</td>
                          <td>Apr-19</td>
                        </tr>
                        <tr>
                          <td>My Calendar</td>
                          <td>Jessica Kowalski</td>
                          <td>Report to the Farm team all upcoming facilities on-call schedules, weekend shifts, and PTO events </td>
                          <td>Active</td>
                          <td>Production</td>
                          <td>Yes</td>
                          <td>Apr-19</td>
                        </tr>
                        <tr>
                          <td>Sensory Results Checklist</td>
                          <td>Molly Kreykes</td>
                          <td>Collect results of sensory testing before product release.</td>
                          <td>Active</td>
                          <td>Production</td>
                          <td>Yes</td>
                          <td>Apr-19</td>
                        </tr>
                        <tr>
                          <td>Systems Checklists</td>
                          <td>Jessica Kowalski</td>
                          <td>Ensure that the correct process is followed in production and research grow rooms and other facilities.</td>
                          <td>Active</td>
                          <td>Production</td>
                          <td>Yes</td>
                          <td>Apr-19</td>
                        </tr>
                        <tr>
                          <td>Work Requests</td>
                          <td>Gantt Charping</td>
                          <td>Support for creating work requests. These are saved as fiix.com tasks</td>
                          <td>Active</td>
                          <td>Production</td>
                          <td>Yes</td>
                          <td>Apr-19</td>
                        </tr>
                        <tr>
                          <td>Handbook</td>
                          <td>Emy Kelty</td>
                          <td>Simple Q&A</td>
                          <td>Active</td>
                          <td>Production</td>
                          <td>No</td>
                          <td>Apr-19</td>
                        </tr>
                        <tr>
                          <td>Empathic Bot</td>
                          <td>Ellipsis</td>
                          <td>Greet users</td>
                          <td>Active</td>
                          <td>Production</td>
                          <td>No</td>
                          <td>Apr-19</td>
                        </tr>
                        <tr>
                          <td>Farm Training</td>
                          <td>Jessica Kowalski</td>
                          <td>Track expired training sessions from a Google sheet and send notifications in Slack reminding people to redo training sessions.</td>
                          <td>Active</td>
                          <td>Production</td>
                          <td>Yes</td>
                          <td>Apr-19</td>
                        </tr>
                        <tr>
                          <td>SF OM Requests</td>
                          <td>Amanda Cabrera</td>
                          <td>Quickly add office management requests made in Slack and store them in a Google Sheet. Allow marking them as complete via Slack to make tracking easier.</td>
                          <td>Inactive</td>
                          <td>Development</td>
                          <td>Yes</td>
                          <td>Apr-19</td>
                        </tr>
                        <tr>
                          <td>Work Request Scheduling</td>
                          <td>Jessica Kowalski</td>
                          <td>Collect work request information for scheduling and planning</td>
                          <td>Inactive</td>
                          <td>Development</td>
                          <td>Yes</td>
                          <td>Apr-19</td>
                        </tr>
                        <tr>
                          <td>Donation Tracker</td>
                          <td>Jessica Kowalski</td>
                          <td>Report how much produce has been donated each week </td>
                          <td>Inactive</td>
                          <td>Requested</td>
                          <td>Yes</td>
                          <td></td>
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
