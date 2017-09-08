define(function(require) {
  const React = require('react'),
    HelpButton = require('../help/help_button'),
    SectionHeading = require('../shared_ui/section_heading'),
    autobind = require('../lib/autobind');

  class DataTypeSourceConfig extends React.Component {
    constructor(props) {
      super(props);
      autobind(this);
    }

    onUseDefaultStorage() {
      this.props.onChange(false);
    }

    onUseCode() {
      this.props.onChange(true);
    }

    toggleSourceHelp() {
      this.props.onToggleActivePanel('helpForDataTypeSource');
    }

    render() {
      return (
        <div className="container ptxl pbxxxl">
          <p className="mbxl">
            Data types are used to store and/or query structured data that can be used as input.
          </p>

          <SectionHeading number="1">
            <span className="mrm">Where should the data come from?</span>
            <span className="display-inline-block">
              <HelpButton onClick={this.toggleSourceHelp} toggled={this.props.activePanelName === 'helpForDataTypeSource'} />
            </span>
          </SectionHeading>

          <div className="mbxl">
            <button className="mrm mbm" type="button" onClick={this.onUseDefaultStorage}>Data stored by Ellipsis</button>
            <button className="mrm mbm" type="button" onClick={this.onUseCode}>Data returned by code</button>
          </div>

        </div>
      );
    }
  }

  DataTypeSourceConfig.propTypes = {
    onChange: React.PropTypes.func.isRequired,
    onToggleActivePanel: React.PropTypes.func.isRequired,
    activePanelName: React.PropTypes.string
  };

  return DataTypeSourceConfig;
});
