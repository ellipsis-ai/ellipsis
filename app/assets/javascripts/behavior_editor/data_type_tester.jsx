define(function(require) {
  var React = require('react'),
    Input = require('../form/input'),
    debounce = require('javascript-debounce'),
    ifPresent = require('../if_present');
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

    getParsedResult: function() {
      try {
        return JSON.parse(this.getResult());
      } catch(e) {
        return null;
      }
    },

    isValidResult: function() {
      var result = this.getParsedResult();
      return Array.isArray(result) && result.every((ea) => {
        return typeof ea === "object" && Object.keys(ea).includes('id') && Object.keys(ea).includes('label');
      });
    },

    isSavedBehavior: function() {
      return !!this.props.behaviorId;
    },

    focus: function() {
      if (this.refs.searchQuery) {
        this.refs.searchQuery.focus();
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
      this.updateResultImmediately();
    },

    onDone: function() {
      this.setState({
        result: ''
      });
      this.props.onDone();
    },

    updateResultImmediately: function() {
      if (this.isSavedBehavior()) {
        this.setState({
          isTesting: true,
          result: ''
        }, this.fetchResult);
      }
    },

    updateResult: debounce(function() {
      this.updateResultImmediately();
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
            result: '',
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
              placeholder="Search query"
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

    renderResultStatus: function() {
      if (this.state.isTesting) {
        return (
          <span className="type-weak pulse">— Testing</span>
        );
      } else if (this.isValidResult()) {
        var numMatches = this.getParsedResult().length;
        return (
          <span className="type-green">— Valid {numMatches === 1 ? '(1 item)' : `(${numMatches} items)`} ✓</span>
        );
      } else if (this.getResult()) {
        return (
          <span className="type-pink">— Invalid: must be an array of objects, each with an <code className="type-black">id</code> and <code className="type-black">label</code> property.</span>
        );
      } else {
        return (
          <span>&nbsp;</span>
        );
      }
    },

    renderResult: function() {
      var isValidResult = this.isValidResult();
      var resultString = this.getResult();
      if (isValidResult) {
        return this.renderValidResultTableWith(this.getParsedResult());
      } else if (resultString) {
        return (
          <pre className="box-code-example">{this.getResult()}</pre>
        );
      } else {
        return (
          <div className="type-weak">No result</div>
        );
      }
    },

    renderValidResultTableWith: function(result) {
      var propertyNameMap = {};
      result.forEach((item) => {
        Object.keys(item).forEach((key) => {
          if (key !== 'id' && key !== 'label') {
            propertyNameMap[key] = true;
          }
        });
      });
      var propertyNames = Object.keys(propertyNameMap);
      return (
        <div className="columns columns-elastic">
          <div className="column-group">
            <div className="column-row">
              <div className="column column-shrink pvxs type-bold">ID</div>
              <div className="column column-expand pvxs type-bold">Label</div>
              {propertyNames.map((name, index) => (
                <div key={`propName${index}`} className="column column-shrink pvxs type-bold">
                  {name}
                </div>
              ))}
            </div>
            {ifPresent(result, () => result.map((item, index) => (
              <div className="column-row">
                <div className="column column-shrink pvxs border-top type-monospace">{item.id}</div>
                <div className="column column-expand pvxs border-top type-monospace">{item.label}</div>
                {propertyNames.map((name, index) => (
                  <div key={`item${index}-propName${index}`} className="column column-shrink pvxs border-top">
                    {item[name] || null}
                  </div>
                ))}
              </div>
            )), () => (
              <div className="column-row">
                <div className="column column-shrink pvxs border-top">—</div>
                <div className="column column-expand pvxs border-top type-italic">No items were returned.</div>
              </div>
            ))}
          </div>
        </div>
      );
    },

    renderIntro: function() {
      if (this.props.isSearch) {
        return (
          <p>
            Type a search query and click Test to run your code and check the result.
          </p>
        );
      } else {
        return (
          <p>
            Click Test to run your code and check the result.
          </p>
        );
      }
    },

    renderTester: function() {
      return (
        <div>
          <div className="mbxl">
            {this.renderIntro()}
            <div className="columns">
              {this.renderSearchQuery()}
              <div className="column column-one-quarter">
                <button type="button" onClick={this.onClick}>Test</button>
              </div>
            </div>
          </div>

          <h4>
            <span>Result </span>
            {this.renderResultStatus()}
          </h4>

          {this.renderResult()}

          <div className="mvxl">
            <button type="button" onClick={this.onDone}>Done</button>
          </div>
        </div>
      );
    }

  });
});
