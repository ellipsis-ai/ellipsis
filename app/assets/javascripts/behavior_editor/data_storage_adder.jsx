define(function(require) {
  const React = require('react');

  class DataStorageAdder extends React.Component {
    render() {
      return (
        <div className="box-action phn">
          <div className="container">
            <div className="columns">
              <div className="column column-page-sidebar">
                <h4>Add data</h4>
              </div>
              <div className="column column-page-main">
                <p>Nothing to see here</p>
              </div>
            </div>
          </div>
        </div>
      );
    }
  }

  DataStorageAdder.propTypes = {
  };

  return DataStorageAdder;
});
