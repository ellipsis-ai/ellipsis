import 'core-js';
import * as React from 'react';
import * as ReactDOM from 'react-dom';
import autobind from '../lib/autobind';
import Page from "../shared_ui/page";
import Dashboard from "./index";
import {Timestamp} from "../lib/formatter";

export interface DashboardDataPointJson {
  t: Timestamp,
  y: number
}

export interface DashboardDataJson {
  installedWorkflows: Array<DashboardDataPointJson>,
  activeWorkflows: Array<DashboardDataPointJson>,
  installedSkills: Array<DashboardDataPointJson>,
  activeSkills: Array<DashboardDataPointJson>,
  createdSkills: Array<DashboardDataPointJson>,
  modifiedSkills: Array<DashboardDataPointJson>,
  totalUsers: Array<DashboardDataPointJson>,
  activeUsers: Array<DashboardDataPointJson>,
  contributingUsers: Array<DashboardDataPointJson>,
  editingUsers: Array<DashboardDataPointJson>
}

export interface DashboardDataPoint extends DashboardDataPointJson {
  t: Date
}

export interface DashboardData {
  installedWorkflows: Array<DashboardDataPoint>,
  activeWorkflows: Array<DashboardDataPoint>,
  installedSkills: Array<DashboardDataPoint>,
  activeSkills: Array<DashboardDataPoint>,
  createdSkills: Array<DashboardDataPoint>,
  modifiedSkills: Array<DashboardDataPoint>,
  totalUsers: Array<DashboardDataPoint>,
  activeUsers: Array<DashboardDataPoint>,
  contributingUsers: Array<DashboardDataPoint>,
  editingUsers: Array<DashboardDataPoint>
}

interface Props {
  containerId: string
  csrfToken: string
  data: DashboardDataJson
}

declare var DashboardConfig: Props;

class DashboardLoader extends React.Component<Props> {
  constructor(props: Props) {
    super(props);
    autobind(this);
  }

  toDataPoint(dataPointJson: DashboardDataPointJson): DashboardDataPoint {
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
        <Dashboard csrfToken={this.props.csrfToken} data={this.getData()} {...pageProps} />
      )} />
    );
  }
}

const container = document.getElementById(DashboardConfig.containerId);
if (container) {
  ReactDOM.render((
    <DashboardLoader {...DashboardConfig} />
  ), container);
}
