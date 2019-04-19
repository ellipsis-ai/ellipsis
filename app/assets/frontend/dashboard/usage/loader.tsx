import 'core-js';
import * as React from 'react';
import * as ReactDOM from 'react-dom';
import autobind from '../../lib/autobind';
import Page from "../../shared_ui/page";
import UsageReport from "./index";
import {Timestamp} from "../../lib/formatter";

export interface ChartDataPointJson {
  t: Timestamp,
  y: number
}

export interface DashboardDataJson {
  installedWorkflows: Array<ChartDataPointJson>,
  activeWorkflows: Array<ChartDataPointJson>,
  installedSkills: Array<ChartDataPointJson>,
  activeSkills: Array<ChartDataPointJson>,
  createdSkills: Array<ChartDataPointJson>,
  modifiedSkills: Array<ChartDataPointJson>,
  totalUsers: Array<ChartDataPointJson>,
  activeUsers: Array<ChartDataPointJson>,
  contributingUsers: Array<ChartDataPointJson>,
  editingUsers: Array<ChartDataPointJson>
}

export interface ChartDataPoint extends ChartDataPointJson {
  t: Date
}

export interface DashboardData {
  installedWorkflows: Array<ChartDataPoint>,
  activeWorkflows: Array<ChartDataPoint>,
  installedSkills: Array<ChartDataPoint>,
  activeSkills: Array<ChartDataPoint>,
  createdSkills: Array<ChartDataPoint>,
  modifiedSkills: Array<ChartDataPoint>,
  totalUsers: Array<ChartDataPoint>,
  activeUsers: Array<ChartDataPoint>,
  contributingUsers: Array<ChartDataPoint>,
  editingUsers: Array<ChartDataPoint>
}

interface Props {
  containerId: string
  csrfToken: string
  data: DashboardDataJson
}

declare var UsageReportConfig: Props;

class UsageReportLoader extends React.Component<Props> {
  constructor(props: Props) {
    super(props);
    autobind(this);
  }

  toDataPoint(dataPointJson: ChartDataPointJson): ChartDataPoint {
    return {
      t: new Date(dataPointJson.t),
      y: dataPointJson.y
    };
  }

  getData(): DashboardData {
    const obj: Partial<DashboardData> = {};
    Object.keys(this.props.data).forEach((k) => {
      obj[k as keyof DashboardData] = this.props.data[k as keyof DashboardDataJson].map(this.toDataPoint);
    });
    return obj as DashboardData;
  }

  render() {
    return (
      <Page csrfToken={this.props.csrfToken} onRender={(pageProps) => (
        <UsageReport csrfToken={this.props.csrfToken} data={this.getData()} {...pageProps} />
      )} />
    );
  }
}

const container = document.getElementById(UsageReportConfig.containerId);
if (container) {
  ReactDOM.render((
    <UsageReportLoader {...UsageReportConfig} />
  ), container);
}
