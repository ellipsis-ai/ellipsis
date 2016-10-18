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
        result: '',
        isTesting: false
      };
    },

    getResult: function() {
      return this.state.result;
    },

    isValidResult: function() {
      try {
        const json = JSON.parse(this.getResult());
        return Array.isArray(json) && json.every((ea) => {
            return typeof ea === "object" && Object.keys(ea).includes('id') && Object.keys(ea).includes('label');
          });
      } catch(e) {
        return false;
      }
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
      this.updateResult();
    },

    onEnterKey: function() {
      this.refs.searchQuery.blur();
      this.updateResult();
    },

    onDone: function() {
      this.setState({
        result: ''
      });
      this.props.onDone();
    },

    updateResult: debounce(function() {
      if (this.isSavedBehavior()) {
        this.setState({
          isTesting: true
        }, this.fetchResult);
      }
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
            result: json.fullText,
            isTesting: false
          });
        })
        .catch(() => {
          this.setState({
            errorOccurred: true,
            isTesting: false
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
          <div className="column column-one-half">
            <Input
              placeholder="Search query:"
              ref="searchQuery"
              value={this.state.searchQuery}
              onChange={this.onChangeSearchQuery}
              onEnterKey={this.onEnterKey}
            />
          </div>
        );
      } else {
        return null;
      }
    },

    renderResultHeading: function() {
      if (this.isValidResult()) {
        return (
          <span className="type-bold type-green">Valid result ✓</span>
        );
      } else {
        return (
          <span className="type-bold type-pink">Invalid result: must be an array of objects, each with a <code>id</code> and <code>label</code> key.</span>
        );
      }
    },

    renderResult: function() {
      if (this.state.isTesting) {
        return (
          <span className="type-weak type-italic pulse">— testing…</span>
        );
      } else if (this.getResult()) {
        return (
          <div>
            <h4 className="mbxs">
              {this.renderResultHeading()}
            </h4>

            <div>
              {this.getResult()}
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

          <div className="mvxl columns">
            {this.renderSearchQuery()}
            <div className="column column-one-quarter">
              <button type="button" onClick={this.onClick}>Test</button>
            </div>
          </div>

          {this.renderResult()}

          <div className="mvxl">
            <button type="button" onClick={this.onDone}>Done</button>
          </div>
        </div>
      );
    }

  });
});
