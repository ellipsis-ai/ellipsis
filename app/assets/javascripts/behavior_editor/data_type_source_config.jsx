define(function(require) {
  var React = require('react'),
    SectionHeading = require('../shared_ui/section_heading');

  return React.createClass({
    displayName: 'DataTypeResultConfig',
    propTypes: {
      onChange: React.PropTypes.func.isRequired
    },

    onUseDefaultStorage: function() {
      this.props.onChange(false);
    },

    onUseCode: function() {
      this.props.onChange(true);
    },

    render: function() {
      return (
        <div className="container ptxl pbxxxl">
          <SectionHeading number="1">Where does the data come from?</SectionHeading>

          <div className="columns">
            <div className="column column-one-half border-right pvm prxxl">
              <div className="mbxl">
                <button type="button" onClick={this.onUseDefaultStorage}>Store data in Ellipsis</button>
              </div>

              <p>Choose <b>Store data in Ellipsis</b> for data you will define and store in a table.</p>

              <p>The data can be added now, or can be collected later.</p>

            </div>
            <div className="column column-one-half border-left mlneg1 pvm plxxl">

              <div className="mbxl">
                <button type="button" onClick={this.onUseCode}>Generate data by code</button>
              </div>

              <p>
                Choose <b>Generate data by code</b> when you want to fetch data from an external API, or generate it dynamically.
              </p>

              <p>
                 The data will not be stored or modified by Ellipsis.
              </p>

            </div>
          </div>
        </div>
      );
    }
  });
});
