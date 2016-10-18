define(function(require) {
  var React = require('react'),
    Input = require('../form/input'),
    debounce = require('javascript-debounce'),
    ifPresent = require('../if_present'),
    Collapsible = require('../collapsible');
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
        isTesting: false,
        hasTested: false
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
      this.props.onDone();
      this.setState(this.getInitialState());
    },

    updateResultImmediately: function() {
      if (this.isSavedBehavior()) {
        this.setState({
          hasTested: true,
          isTesting: true
        }, this.fetchResult);
      }
    },

    updateResult: debounce(function() {
      this.updateResultImmediately();
    }, 250),

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
        <div>
          <Collapsible revealWhen={this.state.hasTested}>
            <div className="box-help">
              <div className="container phn">
                <div className="columns">
                  <div className="column column-one-quarter mobile-column-full"></div>
                  <div className="column column-three-quarters pll mobile-pln mobile-column-full">
                    <Collapsible revealWhen={!!(this.getResult() && !this.state.isTesting)}>
                      {this.renderResult()}
                    </Collapsible>
                    <h4 className="mtl">
                      <span>Test result </span>
                      {this.renderResultStatus()}
                    </h4>
                  </div>
                </div>
              </div>
            </div>
          </Collapsible>
          <div className="box-action">
            <div className="container phn">
              <div className="columns mtl">
                <div className="column column-one-quarter mobile-column-full">
                  <h4 className="type-weak">Test the data type</h4>
                </div>
                <div className="column column-three-quarters pll mobile-pln mobile-column-full">
                  {this.renderTester()}
                </div>
              </div>
            </div>
          </div>
        </div>
      );
    },

    renderSearchQuery: function() {
      if (this.props.isSearch) {
        return (
          <div className="column column-one-half prs">
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
          <span className="type-weak">— No result</span>
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
        return null;
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
            <div className="column-row type-s type-monospace">
              <div className="column column-shrink pvxs">id</div>
              <div className="column column-shrink pvxs">label</div>
              {propertyNames.map((name, index) => (
                <div key={`propName${index}`} className="column column-shrink pvxs">{name}</div>
              ))}
            </div>
            {ifPresent(result, () => result.map((item, itemIndex) => (
              <div className="column-row" key={`item${itemIndex}`}>
                <div className="column column-shrink pvxs border-top"><pre className="box-code-example display-inline-block">{item.id}</pre></div>
                <div className="column column-shrink pvxs border-top"><pre className="box-code-example display-inline-block">{item.label}</pre></div>
                {propertyNames.map((name, propNameIndex) => (
                  <div key={`item${itemIndex}-propName${propNameIndex}`} className="column column-shrink pvxs border-top">
                    {this.renderItemValue(item[name])}
                  </div>
                ))}
              </div>
            )), () => (
              <div className="column-row">
                <div className="column column-shrink pvxs border-top"></div>
                <div className="column column-shrink pvxs border-top type-italic">No items were returned.</div>
              </div>
            ))}
          </div>
        </div>
      );
    },

    renderItemValue: function(value) {
      if (typeof(value) === 'undefined') {
        return (
          <pre className="type-xs paxs type-weak">undefined</pre>
        );
      } else if (typeof(value) === 'string') {
        return (
          <pre className="box-code-example display-inline-block">{value}</pre>
        );
      } else {
        return (
          <pre className="type-xs paxs">{String(value)}</pre>
        );
      }
    },

    renderIntro: function() {
      if (this.props.isSearch) {
        return (
          <p>
            Type a search query, then click Test to run your code and check the result.
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
              <div className="column column-one-half">
                <button className="mrs" type="button" onClick={this.onClick}>Test</button>
                <button type="button" onClick={this.onDone}>Done</button>
              </div>
            </div>
          </div>

        </div>
      );
    }

  });
});
