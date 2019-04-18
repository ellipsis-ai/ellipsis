import * as React from 'react';
import autobind from '../lib/autobind';
import {PageRequiredProps} from "../shared_ui/page";
import {Bar, defaults} from 'react-chartjs-2';
import * as moment from "moment";
import {ChartFontOptions, ChartOptions, ChartPoint} from "chart.js";
import HelpButton from "../help/help_button";
import FixedFooter from "../shared_ui/fixed_footer";
import Collapsible from "../shared_ui/collapsible";
import HelpPanel from "../help/panel";
import {DashboardData, DashboardDataPoint} from "./loader";
import ToggleGroup, {ToggleGroupItem} from "../form/toggle_group";
import {Timestamp} from "../lib/formatter";

const myDefaults = defaults as {
  global: ChartOptions & ChartFontOptions
};
myDefaults.global.defaultFontFamily = "'Source Sans Pro', 'Avenir Next', 'Helvetica Neue', Arial, sans-serif";
myDefaults.global.defaultFontColor = "hsl(235, 14%, 15%)";
myDefaults.global.defaultFontSize = 15;
myDefaults.global.animation = Object.assign(myDefaults.global.animation, {}, {
  duration: 250
});

interface DashboardProps {
  csrfToken: string
  data: DashboardData
}

type Props = DashboardProps & PageRequiredProps

enum TimePeriod {
  Period2018 = "2018",
  Period2019 = "2019",
  PeriodYear = "Last 12 months",
  PeriodAll = "All time"
}

interface State {
  period: TimePeriod
}

class Dashboard extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    autobind(this);
    this.state = {
      period: TimePeriod.PeriodYear
    };
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

  getChartOptions(): ChartOptions {
    return {
      aspectRatio: 2.25,
      maintainAspectRatio: true,
      scales: {
        xAxes: [{
          bounds: 'data',
          type: 'time',
          distribution: 'series',
          time: {
            unit: 'month',
            min: this.getChartMin(),
            max: this.getChartMax(),
            tooltipFormat: 'MMMM YYYY'
          },
          ticks: {
            source: 'data'
          }
        }],
        yAxes: [{
          ticks: {
            beginAtZero: true
          }
        }]
      }
    };
  }

  getChartMin(): string {
    if (this.state.period === TimePeriod.Period2018) {
      return "2017-12-01T00:00:00Z";
    } else if (this.state.period === TimePeriod.Period2019) {
      return "2018-12-01T00:00:00Z";
    } else if (this.state.period === TimePeriod.PeriodYear) {
      return "2018-03-01T00:00:00Z";
    } else {
      return "2017-12-01T00:00:00Z";
    }
  }

  getChartMax(): string {
    if (this.state.period === TimePeriod.Period2018) {
      return "2019-01-01T00:00:00Z";
    } else if (this.state.period === TimePeriod.Period2019) {
      return "2020-01-01T00:00:00Z";
    } else if (this.state.period === TimePeriod.PeriodYear) {
      return "2019-05-01T00:00:00Z";
    } else {
      return "2019-05-01T00:00:00Z";
    }
  }

  getData(key: keyof DashboardData): Array<DashboardDataPoint> {
    const data = this.props.data[key];
    if (this.state.period === TimePeriod.Period2018) {
      return data.slice(0, 12);
    } else if (this.state.period === TimePeriod.Period2019) {
      return data.slice(12);
    } else if (this.state.period === TimePeriod.PeriodYear) {
      return data.slice(data.length - 12);
    } else {
      return data;
    }
  }

  getMainColumnStyle(): React.CSSProperties {
    return {
      paddingBottom: this.props.footerHeight
    };
  }

  setPeriod2018(): void {
    this.setState({
      period: TimePeriod.Period2018
    });
  }

  setPeriod2019(): void {
    this.setState({
      period: TimePeriod.Period2019
    });
  }

  setPeriodYear(): void {
    this.setState({
      period: TimePeriod.PeriodYear
    });
  }

  setPeriodAll(): void {
    this.setState({
      period: TimePeriod.PeriodAll
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
                <div
                  className="column column-page-main column-page-main-wide flex-column flex-column-main position-relative bg-white"
                  style={this.getMainColumnStyle()}
                >
                  <div className="paxxl">

                    <ToggleGroup>
                      <ToggleGroupItem
                        activeWhen={this.state.period === TimePeriod.Period2018}
                        label={"2018"}
                        onClick={this.setPeriod2018}
                      />
                      <ToggleGroupItem
                        activeWhen={this.state.period === TimePeriod.Period2019}
                        label={"2019"}
                        onClick={this.setPeriod2019}
                      />
                      <ToggleGroupItem
                        activeWhen={this.state.period === TimePeriod.PeriodYear}
                        label={"Last 12 months"}
                        onClick={this.setPeriodYear}
                      />
                      <ToggleGroupItem
                        activeWhen={this.state.period === TimePeriod.PeriodAll}
                        label={"All time"}
                        onClick={this.setPeriodAll}
                      />
                    </ToggleGroup>

                    <div className="pvl">
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
                            data: this.getData('installedWorkflows'),
                            fill: false,
                            borderColor: "hsl(231, 97%, 64%)",
                            backgroundColor: "hsla(231, 97%, 64%, 0.1)",
                            borderWidth: 1
                          }, {
                            stack: 'active',
                            label: "Active",
                            type: 'bar',
                            data: this.getData('activeWorkflows'),
                            borderColor: "hsl(231, 97%, 64%)",
                            backgroundColor: "hsl(231, 97%, 64%)"
                          }]
                        }}
                        options={this.getChartOptions()}
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
                            data: this.getData('installedSkills'),
                            fill: false,
                            borderColor: "hsl(231, 97%, 64%)",
                            backgroundColor: "hsla(231, 97%, 64%, 0.1)",
                            borderWidth: 1
                          }, {
                            stack: "active",
                            label: "Active",
                            type: 'bar',
                            data: this.getData('activeSkills'),
                            borderColor: "hsl(231, 97%, 64%)",
                            backgroundColor: "hsl(231, 97%, 64%)"
                          }, {
                            stack: "development",
                            label: "Created",
                            type: 'bar',
                            data: this.getData('createdSkills'),
                            borderColor: "hsl(341, 93%, 60%)",
                            backgroundColor: "hsl(341, 93%, 60%)"
                          }, {
                            stack: "development",
                            label: "Modified",
                            type: 'bar',
                            data: this.getData('modifiedSkills'),
                            borderColor: "hsl(341, 93%, 75%)",
                            backgroundColor: "hsl(341, 93%, 75%)"
                          }]
                        }}
                        options={this.getChartOptions()}
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
                            data: this.getData('totalUsers'),
                            fill: false,
                            borderColor: "hsl(231, 97%, 64%)",
                            backgroundColor: "hsla(231, 97%, 64%, 0.1)",
                            borderWidth: 1
                          }, {
                            stack: "active",
                            label: "Active",
                            type: 'bar',
                            data: this.getData('activeUsers'),
                            borderColor: "hsl(231, 97%, 64%)",
                            backgroundColor: "hsl(231, 97%, 64%)"
                          }, {
                            stack: "contributors",
                            label: "Contributors",
                            type: 'bar',
                            data: this.getData('contributingUsers'),
                            borderColor: "hsl(341, 93%, 60%)",
                            backgroundColor: "hsl(341, 93%, 60%)"
                          }, {
                            stack: "editors",
                            label: "Editors",
                            type: 'bar',
                            data: this.getData('editingUsers'),
                            borderColor: "hsl(341, 93%, 75%)",
                            backgroundColor: "hsl(341, 93%, 75%)"
                          }]
                        }}
                        options={this.getChartOptions()}
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
          <div>
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
          </div>
        ))}
      </div>
    );
  }
}

export default Dashboard;
