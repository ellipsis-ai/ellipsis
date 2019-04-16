import * as React from 'react';
import autobind from '../lib/autobind';
import {PageRequiredProps} from "../shared_ui/page";
import {Bar, defaults} from 'react-chartjs-2';

defaults.global.defaultFontFamily = "'Source Sans Pro', 'Avenir Next', 'Helvetica Neue', Arial, sans-serif";
defaults.global.defaultFontColor = "hsl(235, 14%, 15%)";
interface DashboardProps {
  csrfToken: string
}

type Props = DashboardProps & PageRequiredProps

class Dashboard extends React.Component<Props> {
  constructor(props: Props) {
    super(props);
    autobind(this);
  }

  render() {
    return (
      <div className="flex-row-cascade">
        <div className="flex-columns flex-row-expand">
          <div className="flex-columns flex-row-expand">
            <div className="flex-column flex-column-left flex-rows container container-wide phn">
              <div className="columns flex-columns flex-row-expand mobile-flex-no-columns">
                <div className="column column-page-sidebar flex-column flex-column-left bg-lightest mobile-border-bottom prn">
                  <div className="pvl">
                    Usage report
                  </div>
                </div>
                <div className="column column-page-main column-page-main-wide flex-column flex-column-main position-relative">
                  <div className="pvl prl">
                    <Bar
                      width={null}
                      height={null}
                      data={{
                        datasets: [{
                          label: "Installed",
                          type: 'line',
                          data: [162, 173, 177, 177],
                          fill: false,
                          borderColor: "hsl(231, 22%, 43%)",
                          backgroundColor: "hsl(231, 22%, 43%)"
                        }, {
                          label: "Active",
                          type: 'bar',
                          data: [62, 62, 46, 57],
                          borderColor: "hsl(341, 93%, 65%)",
                          backgroundColor: "hsl(341, 93%, 65%)"
                        }],
                        labels: ["January 2019", "February 2019", "March 2019", "April 2019"],
                      }}
                      options={{
                        aspectRatio: 2.5,
                        maintainAspectRatio: true,
                        title: {
                          display: true,
                          text: "Workflows"
                        },
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
                  <div className="pvl prl">
                    <Bar
                      width={null}
                      height={null}
                      data={{
                        datasets: [{
                          label: "Installed",
                          type: 'line',
                          data: [28, 31, 32, 32],
                          fill: false,
                          borderColor: "hsl(231, 22%, 43%)",
                          backgroundColor: "hsl(231, 22%, 43%)"
                        }, {
                          label: "Active",
                          type: 'bar',
                          data: [13, 15, 14, 13],
                          borderColor: "hsl(231, 97%, 64%)",
                          backgroundColor: "hsl(231, 97%, 64%)"
                        }, {
                          label: "Created",
                          type: 'bar',
                          data: [1, 3, 1, 0],
                          borderColor: "hsl(341, 93%, 60%)",
                          backgroundColor: "hsl(341, 93%, 60%)"
                        }, {
                          label: "Modified",
                          type: 'bar',
                          data: [4, 3, 6, 1],
                          borderColor: "hsl(341, 93%, 70%)",
                          backgroundColor: "hsl(341, 93%, 70%)"
                        }],
                        labels: ["January 2019", "February 2019", "March 2019", "April 2019"],
                      }}
                      options={{
                        aspectRatio: 2.5,
                        maintainAspectRatio: true,
                        title: {
                          display: true,
                          text: "Skills"
                        },
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

export default Dashboard;
