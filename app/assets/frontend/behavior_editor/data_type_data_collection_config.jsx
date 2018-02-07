import * as React from 'react';
import Checklist from './checklist';
import SectionHeading from '../shared_ui/section_heading';
import ToggleGroup from '../form/toggle_group';

class DataTypeDataCollectionConfig extends React.Component {
    // TODO: Hook these up to modify the data type once it supports these options

    setReadOnly() {

    }

    setWriteOnly() {

    }

    setReadWrite() {

    }

    isReadOnly() {
      return true;
    }

    isWriteOnly() {
      return false;
    }

    isReadWrite() {
      return false;
    }

    render() {
      return (
        <div className="container ptxl pbxxl">
          <SectionHeading number={"3"}>Input data collection</SectionHeading>

          <p>
          When collecting an answer for this data type, should it be possible to add new items?
          </p>

          <div className="mvxl">
            <ToggleGroup>
              <ToggleGroup.Item onClick={this.setReadOnly} activeWhen={this.isReadOnly()} label={"Only choose existing items"}/>
              <ToggleGroup.Item onClick={this.setWriteOnly} activeWhen={this.isWriteOnly()} label={"Always add new items"}/>
              <ToggleGroup.Item onClick={this.setReadWrite} activeWhen={this.isReadWrite()} label={"Choose existing or add new"}/>
            </ToggleGroup>
          </div>

          <Checklist disabledWhen={true}>
            <Checklist.Item checkedWhen={this.isReadOnly()}><b>Only choose existing items</b> — people can pick from a list of existing items</Checklist.Item>
            <Checklist.Item checkedWhen={this.isWriteOnly()}><b>Only add new items</b> — people will always be asked to add a new item</Checklist.Item>
            <Checklist.Item checkedWhen={this.isReadWrite()}><b>Choose existing or add new</b> — people will have the choice to pick an existing item or add a new one</Checklist.Item>
          </Checklist>
        </div>
      );
    }
}

DataTypeDataCollectionConfig.propTypes = {
  // string: React.PropTypes.string.isRequired,
  // callback: React.PropTypes.func.isRequired,
  // children: React.PropTypes.node.isRequired
};

export default DataTypeDataCollectionConfig;

