import * as React from 'react';
import autobind from '../../lib/autobind';
import {PageRequiredProps} from "../../shared_ui/page";
import {Bar, defaults} from './chart_component';
import {ChartOptions} from "chart.js";
import HelpButton from "../../help/help_button";
import Collapsible from "../../shared_ui/collapsible";
import HelpPanel from "../../help/panel";
import {DashboardData, ChartDataPoint} from "./loader";
import ToggleGroup, {ToggleGroupItem} from "../../form/toggle_group";
import FixedHeader from "../../shared_ui/fixed_header";

defaults.global.defaultFontFamily = "'Source Sans Pro', 'Avenir Next', 'Helvetica Neue', Arial, sans-serif";
defaults.global.defaultFontColor = "hsl(235, 14%, 15%)";
defaults.global.defaultFontSize = 15;
defaults.global.animation = Object.assign(defaults.global.animation || {}, {
  duration: 250
});

interface UsageProps {
  csrfToken: string
  data: DashboardData
  isAdmin: boolean
  teamId: string
}

type Props = UsageProps & PageRequiredProps

enum TimePeriod {
  Period2018 = "2018",
  Period2019 = "2019",
  PeriodYear = "Last 12 months",
  PeriodAll = "All time"
}

enum Color {
  BlueLight = "hsl(231, 100%, 96%)",
  BlueMedium = "hsl(231, 97%, 64%)",
  PinkMedium = "hsl(341, 93%, 60%)",
  PinkLight = "hsl(341, 93%, 75%)"
}

interface State {
  period: TimePeriod
}

class UsageReport extends React.Component<Props, State> {
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
          scaleLabel: {
            display: true,
            labelString: this.getXAxisScaleLabel()
          },
          bounds: 'data',
          type: 'time',
          distribution: 'series',
          time: {
            unit: 'month',
            displayFormats: {
              month: 'MMM'
            },
            min: this.getChartMin(),
            max: this.getChartMax(),
            round: "month",
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
      return "2017-12-15T00:00:00Z";
    } else if (this.state.period === TimePeriod.Period2019) {
      return "2018-12-15T00:00:00Z";
    } else if (this.state.period === TimePeriod.PeriodYear) {
      return "2018-03-15T00:00:00Z";
    } else {
      return "2017-12-15T00:00:00Z";
    }
  }

  getChartMax(): string {
    if (this.state.period === TimePeriod.Period2018) {
      return "2019-01-15T00:00:00Z";
    } else if (this.state.period === TimePeriod.Period2019) {
      return "2020-01-15T00:00:00Z";
    } else if (this.state.period === TimePeriod.PeriodYear) {
      return "2019-06-15T00:00:00Z";
    } else {
      return "2019-06-15T00:00:00Z";
    }
  }

  getData(key: keyof DashboardData): Array<ChartDataPoint> {
    const data = this.props.data[key];
    if (this.state.period === TimePeriod.Period2018) {
      return data.slice(0, 12);
    } else if (this.state.period === TimePeriod.Period2019) {
      return data.slice(12).concat(this.fill2019());
    } else if (this.state.period === TimePeriod.PeriodYear) {
      return data.slice(data.length - 12);
    } else {
      return data;
    }
  }

  getXAxisScaleLabel(): string {
    if (this.state.period === TimePeriod.Period2018) {
      return "2018";
    } else if (this.state.period === TimePeriod.Period2019) {
      return "2019";
    } else if (this.state.period === TimePeriod.PeriodYear) {
      return "June 2018 – May 2019";
    } else {
      return "January 2018 — May 2019";
    }
  }

  fill2019(): Array<ChartDataPoint> {
    return ["06", "07", "08", "09", "10", "11", "12"].map((ea) => {
      return {
        t: new Date(`2019-${ea}-14T00:00:00Z`),
        y: 0
      };
    });
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

  renderWorkflowActions() {
    return (
      <div>
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
              borderColor: Color.BlueMedium,
              backgroundColor: Color.BlueLight,
              borderWidth: 1
            }, {
              stack: 'active',
              label: "Active",
              type: 'bar',
              data: this.getData('activeWorkflows'),
              borderColor: Color.BlueMedium,
              backgroundColor: "hsl(231, 97%, 64%)"
            }]
          }}
          options={this.getChartOptions()}
        />
      </div>
    )
  }

  renderSkills() {
    return (
      <div>
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
              borderColor: Color.BlueMedium,
              backgroundColor: Color.BlueLight,
              borderWidth: 1
            }, {
              stack: "active",
              label: "Active",
              type: 'bar',
              data: this.getData('activeSkills'),
              borderColor: Color.BlueMedium,
              backgroundColor: Color.BlueMedium
            }, {
              stack: "development",
              label: "Created",
              type: 'bar',
              data: this.getData('createdSkills'),
              borderColor: Color.PinkMedium,
              backgroundColor: Color.PinkMedium
            }, {
              stack: "development",
              label: "Modified",
              type: 'bar',
              data: this.getData('modifiedSkills'),
              borderColor: Color.PinkLight,
              backgroundColor: Color.PinkLight
            }]
          }}
          options={this.getChartOptions()}
        />
      </div>
    )
  }

  renderUsers() {
    return (
      <div>
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
              borderColor: Color.BlueMedium,
              backgroundColor: Color.BlueLight,
              borderWidth: 1
            }, {
              stack: "active",
              label: "Active",
              type: 'bar',
              data: this.getData('activeUsers'),
              borderColor: Color.BlueMedium,
              backgroundColor: Color.BlueMedium
            }, {
              stack: "contributors",
              label: "Contributors",
              type: 'bar',
              data: this.getData('contributingUsers'),
              borderColor: Color.PinkMedium,
              backgroundColor: Color.PinkMedium
            }, {
              stack: "editors",
              label: "Editors",
              type: 'bar',
              data: this.getData('editingUsers'),
              borderColor: Color.PinkLight,
              backgroundColor: Color.PinkLight
            }]
          }}
          options={this.getChartOptions()}
        />

        <p className="type-s type-italic">Active user total available only from March 2019 onwards</p>
      </div>
    );
  }

  render() {
    return (
      <div className="flex-row-cascade">
        <div className="flex-columns flex-row-expand">
          <div className="flex-columns flex-row-expand">
            <div className="flex-column flex-column-left flex-rows container container-wide phn">
              <FixedHeader marginTop={this.props.headerHeight} zIndexClassName="position-z-above">
                <div className="columns flex-columns flex-row-expand mobile-flex-no-columns">
                  <div className="column column-page-sidebar flex-column flex-column-left visibility-hidden prn" />
                  <div className="column column-page-main column-page-main-wide flex-column flex-column-main position-relative bg-white-translucent align-c pvl">
                    <ToggleGroup>
                      <ToggleGroupItem
                        activeWhen={this.state.period === TimePeriod.PeriodYear}
                        label={"Last 12 months"}
                        onClick={this.setPeriodYear}
                      />
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
                        activeWhen={this.state.period === TimePeriod.PeriodAll}
                        label={"All time"}
                        onClick={this.setPeriodAll}
                      />
                    </ToggleGroup>
                  </div>
                </div>
              </FixedHeader>
              <div className="columns flex-columns flex-row-expand mobile-flex-no-columns">
                <div className="column column-page-sidebar flex-column flex-column-left bg-lightest mobile-border-bottom prn">
                  <nav className="mvxxl plxl">
                    <ul className="list-nav">
                      <li className="list-nav-active-item">Usage report</li>
                      <li><a href={jsRoutes.controllers.DashboardController.skillManifest(this.props.isAdmin ? this.props.teamId : null).url}>Skill manifest</a></li>
                    </ul>
                  </nav>
                </div>
                <div
                  className="column column-page-main column-page-main-wide flex-column flex-column-main position-relative bg-white"
                  style={this.getMainColumnStyle()}
                >
                  <div className="ptxxxxl pbxxl phxxl">
                    <div className="pvl">
                      {this.renderSkills()}
                    </div>

                    <div className="pvl">
                      {this.renderWorkflowActions()}
                    </div>

                    <div className="ptl">
                      {this.renderUsers()}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        {this.props.onRenderFooter(this.renderFooter())}
      </div>
    );
  }

  renderFooter() {
    return (
      <div>
        <Collapsible revealWhen={this.props.activePanelName === "helpForWorkflowActions"}>
          <HelpPanel heading={"Workflow actions"} onCollapseClick={this.props.onClearActivePanel}>
            <p>
              A workflow may be comprised of one or more “actions”. Each action represents a function that receives input and provides output, and may be triggered through a variety of ways: by a person typing in chat, on schedule, or in response to some other event.
            </p>

            <div>
              <h5>
                <span className="display-inline-block mrm align-m">
                  <svg width={16} height={16}><rect width={16} height={16} fill={"hsla(231, 97%, 64%, 0.1)"} strokeWidth={1} stroke={Color.BlueMedium} /></svg>
                </span>
                <span>Installed</span>
              </h5>
              <p>The total number of different workflow actions available to be triggered during a time period.</p>

              <h5>
                <span className="display-inline-block mrm align-m">
                  <svg width={16} height={16}><rect width={16} height={16} fill={Color.BlueMedium} /></svg>
                </span>
                <span>Active</span>
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
                  <svg width={16} height={16}><rect width={16} height={16} fill={"hsla(231, 97%, 64%, 0.1)"} strokeWidth={1} stroke={Color.BlueMedium} /></svg>
                </span>
                <span>Installed</span>
              </h5>
              <p>The total number of different skills installed within a time period.</p>

              <h5>
                <span className="display-inline-block mrm align-m">
                  <svg width={16} height={16}><rect width={16} height={16} fill={Color.BlueMedium} /></svg>
                </span>
                <span>Active</span>
              </h5>
              <p>The number of different skills where at least one workflow was triggered during a time period.</p>

              <h5>
                <span className="display-inline-block mrm align-m">
                  <svg width={16} height={16}><rect width={16} height={16} fill={Color.PinkMedium} /></svg>
                </span>
                <span>Created</span>
              </h5>
              <p>The number of new skills that were created during a time period.</p>

              <h5>
                <span className="display-inline-block mrm align-m">
                  <svg width={16} height={16}><rect width={16} height={16} fill={Color.PinkLight} /></svg>
                </span>
                <span>Modified</span>
              </h5>
              <p>The number of existing skills that were edited or updated during a time period.</p>
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
                  <svg width={16} height={16}><rect width={16} height={16} fill={"hsla(231, 97%, 64%, 0.1)"} strokeWidth={1} stroke={Color.BlueMedium} /></svg>
                </span>
                <span>Total in Slack</span>
              </h5>
              <p>The total number of unique users counted in Slack during a time period.</p>

              <h5>
                <span className="display-inline-block mrm align-m">
                  <svg width={16} height={16}><rect width={16} height={16} fill={Color.BlueMedium} /></svg>
                </span>
                <span>Active</span>
              </h5>
              <p>The number of users who belonged to a channel where Ellipsis was triggered during a time period.</p>

              <h5>
                <span className="display-inline-block mrm align-m">
                  <svg width={16} height={16}><rect width={16} height={16} fill={Color.PinkMedium} /></svg>
                </span>
                <span>Contributors</span>
              </h5>
              <p>The number of users who interacted with Ellipsis during a time period by triggering an action, reacting with emoji, or answering a question.</p>

              <h5>
                <span className="display-inline-block mrm align-m">
                  <svg width={16} height={16}><rect width={16} height={16} fill={Color.PinkLight} /></svg>
                </span>
                <span>Editors</span>
              </h5>
              <p>The number of users who created or modified any skill, schedule, or other Ellipsis setting during a time period.</p>
            </div>
          </HelpPanel>
        </Collapsible>
      </div>
    )
  }
}

export default UsageReport;
