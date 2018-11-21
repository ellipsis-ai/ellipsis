import * as React from 'react';
import HelpButton from '../help/help_button';
import SectionHeading from '../shared_ui/section_heading';
import autobind from '../lib/autobind';
import Button from "../form/button";

interface Props {
  onChange: (usesCode: boolean) => void,
  onToggleActivePanel: (panelName: string) => void,
  activePanelName: string
}

class DataTypeSourceConfig extends React.PureComponent<Props> {
    constructor(props: Props) {
      super(props);
      autobind(this);
    }

    onUseDefaultStorage(): void {
      this.props.onChange(false);
    }

    onUseCode(): void {
      this.props.onChange(true);
    }

    toggleSourceHelp(): void {
      this.props.onToggleActivePanel('helpForDataTypeSource');
    }

    render() {
      return (
        <div className="container ptxl pbxxxl">
          <p className="mbxl">
            Data types are used to store and/or query structured data that can be used as input.
          </p>

          <SectionHeading number="1">
            <span className="mrm">Data source</span>
            <span className="display-inline-block">
              <HelpButton onClick={this.toggleSourceHelp} toggled={this.props.activePanelName === 'helpForDataTypeSource'} />
            </span>
          </SectionHeading>

          <div className="mbxl">
            <Button className="mrm mbm" onClick={this.onUseDefaultStorage}>Data stored by Ellipsis</Button>
            <Button className="mrm mbm" onClick={this.onUseCode}>Data returned by code</Button>
          </div>

        </div>
      );
    }
}

export default DataTypeSourceConfig;
