define(function(require) {
  var React = require('react'),
    BehaviorTest = require('./behavior_test'),
    Input = require('../form/input'),
    debounce = require('javascript-debounce'),
    Collapsible = require('../collapsible');
  require('whatwg-fetch');

  var MAX_RESULTS_TO_SHOW = 10;

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

    params: function() {
      if (this.state.searchQuery) {
        return { searchQuery: this.state.searchQuery };
      } else {
        return {};
      }
    },

    fetchResult: function() {
      BehaviorTest.testInvocation({
        behaviorId: this.props.behaviorId,
        csrfToken: this.props.csrfToken,
        paramValues: this.params(),
        onSuccess: (json) => {
          this.setState({
            result: json.result.fullText,
            isTesting: false
          });
        },
        onError: () => {
          this.setState({
            result: '',
            errorOccurred: true,
            isTesting: false
          });
        }
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
          <Input
            className="width-20 mrs mbs"
            placeholder="Search query"
            ref="searchQuery"
            value={this.state.searchQuery}
            onChange={this.onChangeSearchQuery}
            onEnterKey={this.onEnterKey}
          />
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
        return (
          <div/>
        );
      }
    },

    renderValidResultTableWith: function(result) {
      var hasOtherData = result.some((item) => {
        return Object.keys(item).filter((key) => {
          return key !== 'id' && key !== 'label';
        }).length > 0;
      });
      var overflowResults = result.length - MAX_RESULTS_TO_SHOW;
      if (result.length > 0) {
        return (
          <div>
            <div className="columns columns-elastic">
              <div className="column-group">
                <div className="column-row type-s type-monospace">
                  <div className="column column-shrink pbxs">id</div>
                  <div className="column column-shrink pbxs">label</div>
                  <div className="column column-expand pbxs">
                    {hasOtherData ? "Other properties" : ""}
                  </div>
                </div>
                {result.slice(0, MAX_RESULTS_TO_SHOW).map((item, itemIndex) => (
                  <div className="column-row" key={`item${itemIndex}`}>
                    <div className="column column-shrink pbxs">
                      <pre className="box-code-example display-inline-block">{item.id}</pre>
                    </div>
                    <div className="column column-shrink pbxs">
                      <pre className="box-code-example display-inline-block">{item.label}</pre>
                    </div>
                    <div className="column column-expand pbxs">
                      {hasOtherData ? this.renderOtherDataForItem(item) : null}
                    </div>
                  </div>
                ))}
              </div>
            </div>
            {overflowResults > 0 ? (
              <div className="ptxs type-italic type-s type-weak">
                {overflowResults === 1 ?
                  "1 more item not shown" :
                  `${overflowResults} more items not shown`
                }
              </div>
            ) : null}
          </div>
        );
      } else {
        return (
          <div className="type-italic">An empty array was returned.</div>
        );
      }
    },

    renderOtherDataForItem: function(item) {
      var copy = Object.assign({}, item);
      delete copy.id;
      delete copy.label;
      return (
        <div className="box-code-example display-limit-width display-ellipsis type-monospace">{JSON.stringify(copy, null, 1)}</div>
      );
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
            <div className="columns columns-elastic">
              <div className="column column-expand">
                {this.renderSearchQuery()}
                <button className="button-primary mbs" type="button" onClick={this.onClick}>Test</button>
              </div>
              <div className="column column-shrink align-b">
                <button className="mbs" type="button" onClick={this.onDone}>Done</button>
              </div>
            </div>
          </div>

        </div>
      );
    }

  });
});
