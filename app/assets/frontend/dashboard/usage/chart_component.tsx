import * as React from 'react';
import * as ChartJs from 'chart.js';
import DeepEqual from "../../lib/deep_equal";
import {Chart, ChartPoint} from "chart.js";
import autobind from "../../lib/autobind";

interface Props {
  data: ChartJs.ChartData;
  type?: ChartJs.ChartType;
  getDatasetAtEvent?: (elems: Array<{}>, e: any) => void;
  getElementAtEvent?: (elems: [{}], e: any) => void;
  getElementsAtEvent?: (elems: Array<{}>, e: any) => void;
  height?: number | null;
  legend?: ChartJs.ChartLegendOptions;
  onElementsClick?(elems: Array<{}>, e: any): void; // alias for getElementsAtEvent (backward compatibility)
  options?: ChartJs.ChartOptions;
  plugins?: object[];
  redraw?: boolean;
  width?: number | null;
  datasetKeyProvider?: (d: ChartJs.ChartDataSets) => string;
  id?: string
}

type ChartClass = Chart & {
  options?: ChartJs.ChartOptions
}

interface WithChartInstance {
  chartInstance: Option<ChartClass>;
}

class ChartComponent extends React.Component<Props> implements WithChartInstance {
  chartInstance: Option<ChartClass>;
  element: Option<HTMLCanvasElement>;
  shadowDataProp: ChartJs.ChartData & {
    datasets?: Array<ChartJs.ChartDataSets>;
  };
  datasets: {
    [k: string]: ChartJs.ChartDataSets
  };

  static getLabelAsKey = (d: ChartJs.ChartDataSets) => d.label;

  static defaultProps = {
    legend: {
      display: true,
      position: 'bottom'
    },
    type: 'doughnut',
    redraw: false,
    options: {},
    datasetKeyProvider: ChartComponent.getLabelAsKey
  };

  constructor(props: Props) {
    super(props);
    autobind(this);
  }


  componentWillMount() {
    this.chartInstance = undefined;
  }

  componentDidMount() {
    this.renderChart();
  }

  componentDidUpdate() {
    if (this.props.redraw) {
      this.destroyChart();
      this.renderChart();
      return;
    }

    this.updateChart();
  }

  shouldComponentUpdate(nextProps: Props) {
    const {
      redraw,
      type,
      options,
      plugins,
      legend,
      height,
      width
    } = this.props;

    if (nextProps.redraw === true) {
      return true;
    }

    if (height !== nextProps.height || width !== nextProps.width) {
      return true;
    }

    if (type !== nextProps.type) {
      return true;
    }

    if (!DeepEqual.isEqual(legend, nextProps.legend)) {
      return true;
    }

    if (!DeepEqual.isEqual(options, nextProps.options)) {
      return true;
    }

    const nextData = this.transformDataProp(nextProps);

    if( !DeepEqual.isEqual(this.shadowDataProp, nextData)) {
      return true;
    }

    return !DeepEqual.isEqual(plugins, nextProps.plugins);

  }

  componentWillUnmount() {
    this.destroyChart();
  }

  transformDataProp(props: Props) {
    const { data } = props;
    return data;
  }

  // Chart.js directly mutates the data.dataset objects by adding _meta proprerty
  // this makes impossible to compare the current and next data changes
  // therefore we memoize the data prop while sending a fake to Chart.js for mutation.
  // see https://github.com/chartjs/Chart.js/blob/master/src/core/core.controller.js#L615-L617
  memoizeDataProps() {
    if (!this.props.data) {
      return;
    }

    const data = this.transformDataProp(this.props);

    this.shadowDataProp = {
      ...data,
      datasets: data.datasets && data.datasets.map(set => {
        return {
          ...set
        };
      })
    };

    this.saveCurrentDatasets(); // to remove the dataset metadata from this chart when the chart is destroyed

    return data;
  }

  getCurrentDatasets() {
    return (this.chartInstance && this.chartInstance.config.data && this.chartInstance.config.data.datasets) || [];
  }

  getDatasetKeyProvider() {
    return this.props.datasetKeyProvider || ChartComponent.getLabelAsKey;
  }

  saveCurrentDatasets() {
    this.datasets = this.datasets || {};
    var currentDatasets = this.getCurrentDatasets();
    currentDatasets.forEach(d => {
      const key = this.getDatasetKeyProvider()(d);
      if (key) {
        this.datasets[key] = d;
      }
    })
  }

  updateChart() {
    const {options} = this.props;

    const data = this.memoizeDataProps();

    if (!this.chartInstance) return;

    if (options) {
      this.chartInstance.options = ChartJs.helpers.configMerge(this.chartInstance.options, options);
    }

    // Pipe datasets to chart instance datasets enabling
    // seamless transitions
    const currentDatasets = this.getCurrentDatasets();
    const nextDatasets = data && data.datasets || [];

    const currentDatasetsIndexed: {
      [k: string]: ChartJs.ChartDataSets
    } = {};
    currentDatasets.forEach((ea) => {
      const key = this.getDatasetKeyProvider()(ea);
      if (key) {
        currentDatasetsIndexed[key] = ea;
      }
    });

    // We can safely replace the dataset array, as long as we retain the _meta property
    // on each dataset.
    if (this.chartInstance.config.data) {
      this.chartInstance.config.data.datasets = nextDatasets.map(next => {
        const key = this.getDatasetKeyProvider()(next);
        const current = key ? currentDatasetsIndexed[key] : null;

        if (current && current.type === next.type) {
          // The data array must be edited in place. As chart.js adds listeners to it.
          if (current.data && next.data) {
            current.data.splice(next.data.length);
            next.data.forEach((point: number | null | undefined | ChartPoint, pid: number) => {
              if (current.data && next.data) {
                current.data[pid] = next.data[pid];
              }
            });
          }
          const { data, ...otherProps } = next;
          // Merge properties. Notice a weakness here. If a property is removed
          // from next, it will be retained by current and never disappears.
          // Workaround is to set value to null or undefined in next.
          return {
            ...current,
            ...otherProps
          };
        } else {
          return next;
        }
      });
    }

    if (data) {
      const { datasets, ...rest } = data;
      this.chartInstance.config.data = {
        ...this.chartInstance.config.data,
        ...rest
      };
    }

    this.chartInstance.update();
  }

  renderChart() {
    const {options, legend, type, plugins} = this.props;
    const node = this.element as HTMLCanvasElement;
    const data = this.memoizeDataProps();

    if(typeof legend !== 'undefined' && !DeepEqual.isEqual(ChartComponent.defaultProps.legend, legend)) {
      Object.assign(options, {}, {
        legend: legend
      });
    }

    this.chartInstance = new Chart(node, {
      type,
      data,
      options,
      plugins
    });
  }

  destroyChart() {
    // Put all of the datasets that have existed in the chart back on the chart
    // so that the metadata associated with this chart get destroyed.
    // This allows the datasets to be used in another chart. This can happen,
    // for example, in a tabbed UI where the chart gets created each time the
    // tab gets switched to the chart and uses the same data).
    this.saveCurrentDatasets();
    const datasets = Object.keys(this.datasets).map((k) => this.datasets[k]);
    if (this.chartInstance) {
      if (this.chartInstance.config.data) {
        this.chartInstance.config.data.datasets = datasets;
      }
      this.chartInstance.destroy();
    }
  }

  handleOnClick = (event: React.MouseEvent<HTMLCanvasElement>) => {
    const instance = this.chartInstance;

    const {
      getDatasetAtEvent,
      getElementAtEvent,
      getElementsAtEvent,
      onElementsClick
    } = this.props;

    if (instance) {
      getDatasetAtEvent && getDatasetAtEvent(instance.getDatasetAtEvent(event), event);
      getElementAtEvent && getElementAtEvent(instance.getElementAtEvent(event), event);
      getElementsAtEvent && getElementsAtEvent(instance.getElementsAtEvent(event), event);
      onElementsClick && onElementsClick(instance.getElementsAtEvent(event), event); // Backward compatibility
    }
  };

  ref = (element: HTMLCanvasElement) => {
    this.element = element;
  };

  render() {
    const {height, width, id} = this.props;

    return (
      <canvas
        ref={this.ref}
        height={height || undefined}
        width={width || undefined}
        id={id}
        onClick={this.handleOnClick}
      />
    );
  }
}

export default ChartComponent;

export class Doughnut extends React.Component<Props> implements WithChartInstance {
  chartInstance: Option<ChartClass>;
  render() {
    return (
      <ChartComponent
        {...this.props}
        ref={ref => this.chartInstance = ref && ref.chartInstance}
        type='doughnut'
      />
    );
  }
}

export class Pie extends React.Component<Props> implements WithChartInstance {
  chartInstance: Option<ChartClass>;
  render() {
    return (
      <ChartComponent
        {...this.props}
        ref={ref => this.chartInstance = ref && ref.chartInstance}
        type='pie'
      />
    );
  }
}

export class Line extends React.Component<Props> implements WithChartInstance {
  chartInstance: Option<ChartClass>;
  render() {
    return (
      <ChartComponent
        {...this.props}
        ref={ref => this.chartInstance = ref && ref.chartInstance}
        type='line'
      />
    );
  }
}

export class Bar extends React.Component<Props> implements WithChartInstance {
  chartInstance: Option<ChartClass>;
  render() {
    return (
      <ChartComponent
        {...this.props}
        ref={ref => this.chartInstance = ref && ref.chartInstance}
        type='bar'
      />
    );
  }
}

export class HorizontalBar extends React.Component<Props> implements WithChartInstance {
  chartInstance: Option<ChartClass>;
  render() {
    return (
      <ChartComponent
        {...this.props}
        ref={ref => this.chartInstance = ref && ref.chartInstance}
        type='horizontalBar'
      />
    );
  }
}

export class Radar extends React.Component<Props> implements WithChartInstance {
  chartInstance: Option<ChartClass>;
  render() {
    return (
      <ChartComponent
        {...this.props}
        ref={ref => this.chartInstance = ref && ref.chartInstance}
        type='radar'
      />
    );
  }
}

export class Polar extends React.Component<Props> implements WithChartInstance {
  chartInstance: Option<ChartClass>;
  render() {
    return (
      <ChartComponent
        {...this.props}
        ref={ref => this.chartInstance = ref && ref.chartInstance}
        type='polarArea'
      />
    );
  }
}

export class Bubble extends React.Component<Props> implements WithChartInstance {
  chartInstance: Option<ChartClass>;
  render() {
    return (
      <ChartComponent
        {...this.props}
        ref={ref => this.chartInstance = ref && ref.chartInstance}
        type='bubble'
      />
    );
  }
}

export class Scatter extends React.Component<Props> implements WithChartInstance {
  chartInstance: Option<ChartClass>;
  render() {
    return (
      <ChartComponent
        {...this.props}
        ref={ref => this.chartInstance = ref && ref.chartInstance}
        type='scatter'
      />
    );
  }
}

export const defaults = Chart.defaults;
export {Chart};
