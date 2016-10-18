define(function(require) {
  var React = require('react'),
    Input = require('../form/input'),
    debounce = require('javascript-debounce');
  require('whatwg-fetch');

  return React.createClass({
    displayName: 'DataTypeTester',
    propTypes: {
      behaviorId: React.PropTypes.string.isRequired,
      isSearch: React.PropTypes.bool,
      csrfToken: React.PropTypes.string.isRequired,
      onDone: React.PropTypes.func.isRequired
    },

    getInitialState: function() {
      return {
        searchQuery: '',
        result: ''
      };
    },

    getResult: function() {
      return this.state.result;
    },

    isSavedBehavior: function() {
      return !!this.props.behaviorId;
    },

    focus: function() {
      if (this.refs.testMessage) {
        this.refs.testMessage.focus();
      }
    },

    onChangeSearchQuery: function(value) {
      this.setState({
        searchQuery: value
      });
    },

    onClick: function() {
      if (this.isSavedBehavior()) {
        this.updateResult();
      }
    },

    updateResult: debounce(function() {
      this.setState({
        isTesting: true
      }, this.fetchResult);
    }, 500),

    fetchResult: function() {
      var formData = new FormData();
      formData.append('behaviorId', this.props.behaviorId);
      formData.append('paramValuesJson', JSON.stringify([this.state.searchQuery]));
      fetch(jsRoutes.controllers.BehaviorEditorController.testInvocation().url, {
        credentials: 'same-origin',
        method: 'POST',
        headers: {
          'Accept': 'application/json',
          'Csrf-Token': this.props.csrfToken
        },
        body: formData
      })
        .then((response) => response.json())
        .then((json) => {
          this.setState({
            result: json.fullText
          });
        })
        .catch(() => {
          this.setState({
            errorOccurred: true
          });
        });
    },

    render: function() {
      return (
        <div className="box-action">
          <div className="container phn">
            <div className="columns">
              <div className="column column-one-quarter mobile-column-full">
                <h4 className="type-weak">Test the data type</h4>
              </div>
              <div className="column column-three-quarters pll mobile-pln mobile-column-full">
                {this.renderTester()}
              </div>
            </div>
          </div>
        </div>
      );
    },

    renderSearchQuery: function() {
      if (this.props.isSearch) {
        return (
          <div className="columns pvm">
            <div className="column column-one-half">
              <Input placeholder="Search query:" ref="searchQuery" value={this.state.searchQuery} onChange={this.onChangeSearchQuery}/>
            </div>
          </div>
        );
      } else {
        return null;
      }
    },

    renderTester: function() {
      return (
        <div>

          <div className="mvxl">
            {this.renderSearchQuery()}
            <button type="button" onClick={this.onClick}>Test</button>
          </div>

          <h4 className="mbxs">
            <span>Result</span>
          </h4>

          <div>
           {this.getResult()}
          </div>

          <div className="mvxl">
            <button type="button" onClick={this.props.onDone}>Done</button>
          </div>
        </div>
      );
    }

  });
});
