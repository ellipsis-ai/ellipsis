import * as React from 'react';
import autobind from '../lib/autobind';
import {PageRequiredProps} from "../shared_ui/page";
import {Bar, defaults} from 'react-chartjs-2';
import * as moment from "moment";
import {ChartPoint} from "chart.js";
import HelpButton from "../help/help_button";
import FixedFooter from "../shared_ui/fixed_footer";
import Collapsible from "../shared_ui/collapsible";
import HelpPanel from "../help/panel";

defaults.global.defaultFontFamily = "'Source Sans Pro', 'Avenir Next', 'Helvetica Neue', Arial, sans-serif";
defaults.global.defaultFontColor = "hsl(235, 14%, 15%)";
defaults.global.animation.duration = 250;

interface DashboardProps {
  csrfToken: string
}

type Props = DashboardProps & PageRequiredProps

class Dashboard extends React.Component<Props> {
  constructor(props: Props) {
    super(props);
    autobind(this);
  }

  toggleHelpForWorkflowActions(): void {
    this.props.onToggleActivePanel("helpForWorkflowActions");
  }

  toggleHelpForSkills(): void {
    this.props.onToggleActivePanel("helpForSkills");
  }

  toggleHelpForUsers(): void {
    this.props.onToggleActivePanel("helpForUsers");
  }

  dataToTimeSeries(data: Array<number | null>): Array<ChartPoint> {
    return data.fill(null, data.length, 12).map((item: number | null, index: number) => {
      return {
        t: typeof(item) === "number" ? moment(`2019-${index + 1}-1`, 'YYYY-M-D').toDate() : undefined,
        y: typeof(item) === "number" ? item : undefined
      };
    });
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
                      <li className="list-nav-active-item">Usage report</li>
                    </ul>
                  </nav>
                </div>
                <div className="column column-page-main column-page-main-wide flex-column flex-column-main position-relative bg-white">
                  <div className="paxxl">
                    <div className="pbl">
                      <h4 className="align-c">
                        <span className="mrm">Workflow actions</span>
                        <HelpButton onClick={this.toggleHelpForWorkflowActions} toggled={this.props.activePanelName === "helpForWorkflowActions"} />
                      </h4>
                      <Bar
                        width={null}
                        height={null}
                        data={{
                          datasets: [{
                            stack: 'installed',
                            label: "Installed",
                            type: 'bar',
                            data: this.dataToTimeSeries([162, 173, 177, 177]),
                            fill: false,
                            borderColor: "hsl(341, 93%, 65%)",
                            backgroundColor: "hsla(341, 93%, 65%, 0.1)",
                            borderWidth: 1
                          }, {
                            stack: 'active',
                            label: "Active",
                            type: 'bar',
                            data: this.dataToTimeSeries([62, 62, 46, 57, 0, 0, 0, 0, 0, 0, 0, 0]),
                            borderColor: "hsl(341, 93%, 65%)",
                            backgroundColor: "hsl(341, 93%, 65%)"
                          }],
                          labels: ["Jan 2019", "Feb 2019", "March 2019", "April 2019", "May 2019", "June 2019",
                          "July 2019", "Aug 2019", "Sept 2019", "Oct 2019", "Nov 2019", "Dec 2019"]
                        }}
                        options={{
                          aspectRatio: 2.5,
                          maintainAspectRatio: true,
                          scales: {
                            yAxes: [{
                              ticks: {
                                beginAtZero: true
                              }
                            }]
                          }
                        }}
                      />
                    </div>

                    <div className="pvl">
                      <h4 className="align-c">
                        <span className="mrm">Skills</span>
                        <HelpButton onClick={this.toggleHelpForSkills} toggled={this.props.activePanelName === "helpForSkills"} />
                      </h4>
                      <Bar
                        width={null}
                        height={null}
                        data={{
                          datasets: [{
                            stack: "installed",
                            label: "Installed",
                            type: 'bar',
                            data: this.dataToTimeSeries([28, 31, 32, 32]),
                            fill: false,
                            borderColor: "hsl(231, 97%, 64%)",
                            backgroundColor: "hsla(231, 97%, 64%, 0.1)",
                            borderWidth: 1
                          }, {
                            stack: "active",
                            label: "Active",
                            type: 'bar',
                            data: this.dataToTimeSeries([13, 15, 14, 13, 0, 0, 0, 0, 0, 0, 0, 0]),
                            borderColor: "hsl(231, 97%, 64%)",
                            backgroundColor: "hsl(231, 97%, 64%)"
                          }, {
                            stack: "development",
                            label: "Created",
                            type: 'bar',
                            data: this.dataToTimeSeries([1, 3, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0]),
                            borderColor: "hsl(341, 93%, 60%)",
                            backgroundColor: "hsl(341, 93%, 60%)"
                          }, {
                            stack: "development",
                            label: "Modified",
                            type: 'bar',
                            data: this.dataToTimeSeries([4, 3, 6, 1, 0, 0, 0, 0, 0, 0, 0, 0]),
                            borderColor: "hsl(341, 93%, 75%)",
                            backgroundColor: "hsl(341, 93%, 75%)"
                          }],
                          labels: ["Jan 2019", "Feb 2019", "March 2019", "April 2019", "May 2019", "June 2019",
                            "July 2019", "Aug 2019", "Sept 2019", "Oct 2019", "Nov 2019", "Dec 2019"]
                        }}
                        options={{
                          aspectRatio: 2.5,
                          maintainAspectRatio: true,
                          scales: {
                            yAxes: [{
                              ticks: {
                                beginAtZero: true
                              }
                            }]
                          }
                        }}
                      />
                    </div>

                    <div className="ptl">
                      <h4 className="align-c">
                        <span className="mrm">Users</span>
                        <HelpButton onClick={this.toggleHelpForUsers} toggled={this.props.activePanelName === "helpForUsers"} />
                      </h4>
                      <Bar
                        width={null}
                        height={null}
                        data={{
                          datasets: [{
                            stack: "slack",
                            label: "Total in Slack",
                            type: 'bar',
                            data: this.dataToTimeSeries([215, 218, 225, 225]),
                            fill: false,
                            borderColor: "hsl(231, 97%, 64%)",
                            backgroundColor: "hsla(231, 97%, 64%, 0.1)",
                            borderWidth: 1
                          }, {
                            stack: "active",
                            label: "Active",
                            type: 'bar',
                            data: this.dataToTimeSeries([null, null, 198, 199, 0, 0, 0, 0, 0, 0, 0, 0]),
                            borderColor: "hsl(231, 97%, 64%)",
                            backgroundColor: "hsl(231, 97%, 64%)"
                          }, {
                            stack: "contributors",
                            label: "Contributors",
                            type: 'bar',
                            data: this.dataToTimeSeries([65, 67, 84, 66, 0, 0, 0, 0, 0, 0, 0, 0]),
                            borderColor: "hsl(341, 93%, 60%)",
                            backgroundColor: "hsl(341, 93%, 60%)"
                          }, {
                            stack: "editors",
                            label: "Editors",
                            type: 'bar',
                            data: this.dataToTimeSeries([0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0]),
                            borderColor: "hsl(341, 93%, 75%)",
                            backgroundColor: "hsl(341, 93%, 75%)"
                          }],
                          labels: ["Jan 2019", "Feb 2019", "March 2019", "April 2019", "May 2019", "June 2019",
                            "July 2019", "Aug 2019", "Sept 2019", "Oct 2019", "Nov 2019", "Dec 2019"]
                        }}
                        options={{
                          aspectRatio: 2.5,
                          maintainAspectRatio: true,
                          scales: {
                            yAxes: [{
                              ticks: {
                                beginAtZero: true
                              }
                            }]
                          }
                        }}
                      />

                      <p className="type-s type-italic">Active user total available only from March 2019 onwards</p>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        {this.props.onRenderFooter((
          <FixedFooter>
            <Collapsible revealWhen={this.props.activePanelName === "helpForWorkflowActions"}>
              <HelpPanel heading={"Workflow actions"} onCollapseClick={this.props.onClearActivePanel}>
                <p>
                  A workflow may be comprised of one or more “actions”. Each action represents a function that receives input and provides output, and may be triggered through a variety of ways: by a person typing in chat, on schedule, or in response to some other event.
                </p>

                <div>
                  <h5>
                    <span className="display-inline-block mrm align-m">
                      <svg width={16} height={16}><rect width={16} height={16} fill={"hsla(231, 97%, 64%, 0.1)"} strokeWidth={1} stroke={"hsl(231, 97%, 64%)"} /></svg>
                    </span>
                    Installed
                  </h5>
                  <p>The total number of different workflow actions available to be triggered during a time period.</p>

                  <h5>
                    <span className="display-inline-block mrm align-m">
                      <svg width={16} height={16}><rect width={16} height={16} fill={"hsl(231, 97%, 64%)"} /></svg>
                    </span>
                    Active
                  </h5>
                  <p>The number of different workflow actions that were triggered during a time period.</p>
                </div>
              </HelpPanel>
            </Collapsible>

            <Collapsible revealWhen={this.props.activePanelName === "helpForSkills"}>
              <HelpPanel heading={"Skills"} onCollapseClick={this.props.onClearActivePanel}>
                <p>
                  A skill is a package of one or more related workflows, installed, developed and modified as a group.
                </p>

                <div>
                  <h5>
                    <span className="display-inline-block mrm align-m">
                      <svg width={16} height={16}><rect width={16} height={16} fill={"hsla(231, 97%, 64%, 0.1)"} strokeWidth={1} stroke={"hsl(231, 97%, 64%)"} /></svg>
                    </span>
                    <span>Installed</span>
                  </h5>
                  <p>The total number of different skills installed within a time period.</p>

                  <h5>
                    <span className="display-inline-block mrm align-m">
                      <svg width={16} height={16}><rect width={16} height={16} fill={"hsl(231, 97%, 64%)"} /></svg>
                    </span>
                    <span>Active</span>
                  </h5>
                  <p>The number of different skills where at least one workflow was triggered during a time period.</p>
                </div>
              </HelpPanel>
            </Collapsible>

            <Collapsible revealWhen={this.props.activePanelName === "helpForUsers"}>
              <HelpPanel heading={"Users"} onCollapseClick={this.props.onClearActivePanel}>
                <p>
                  Users are people who have access to use Ellipsis through a medium like chat.
                </p>

                <div>
                  <h5>
                    <span className="display-inline-block mrm align-m">
                      <svg width={16} height={16}><rect width={16} height={16} fill={"hsla(231, 97%, 64%, 0.1)"} strokeWidth={1} stroke={"hsl(231, 97%, 64%)"} /></svg>
                    </span>
                    <span>Total in Slack</span>
                  </h5>
                  <p>The total number of unique users counted in Slack during a time period.</p>

                  <h5>
                    <span className="display-inline-block mrm align-m">
                      <svg width={16} height={16}><rect width={16} height={16} fill={"hsl(231, 97%, 64%)"} /></svg>
                    </span>
                    <span>Active</span>
                  </h5>
                  <p>The number of users who belonged to a channel where Ellipsis was triggered during a time period.</p>

                  <h5>
                    <span className="display-inline-block mrm align-m">
                      <svg width={16} height={16}><rect width={16} height={16} fill={"hsl(341, 93%, 60%)"} /></svg>
                    </span>
                    <span>Contributors</span>
                  </h5>
                  <p>The number of users who interacted with Ellipsis during a time period by triggering an action, reacting with emoji, or answering a question.</p>

                  <h5>
                    <span className="display-inline-block mrm align-m">
                      <svg width={16} height={16}><rect width={16} height={16} fill={"hsl(341, 93%, 75%)"} /></svg>
                    </span>
                    <span>Editors</span>
                  </h5>
                  <p>The number of users who created or modified any skill, schedule, or other Ellipsis setting during a time period.</p>
                </div>
              </HelpPanel>
            </Collapsible>
          </FixedFooter>
        ))}
      </div>
    );
  }
}

export default Dashboard;
