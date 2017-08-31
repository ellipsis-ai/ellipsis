define(function(require) {
  var React = require('react'),
    BehaviorTest = require('./behavior_test'),
    Input = require('../form/input'),
    Collapsible = require('../shared_ui/collapsible'),
    oauth2ApplicationShape = require('./oauth2_application_shape'),
    TesterAuthRequired = require('./tester_auth_required'),
    InvocationResults = require('./behavior_tester_invocation_results'),
    InvocationTestResult = require('../models/behavior_invocation_result');

  var MAX_RESULTS_TO_SHOW = 10;

  return React.createClass({
    displayName: 'DataTypeTester',
    propTypes: {
      behaviorId: React.PropTypes.string,
      isSearch: React.PropTypes.bool,
      csrfToken: React.PropTypes.string.isRequired,
      onDone: React.PropTypes.func.isRequired,
      appsRequiringAuth: React.PropTypes.arrayOf(oauth2ApplicationShape).isRequired
    },

    getInitialState: function() {
      return {
        searchQuery: '',
        results: [],
        isTesting: false,
        hasTested: false
      };
    },

    getResults: function() {
      return this.state.results;
    },

    getParsedResponse: function(result) {
      try {
        return JSON.parse(result.responseText);
      } catch(e) {
        return null;
      }
    },

    isValidResult: function(result) {
      var parsedResponse = this.getParsedResponse(result);
      return Array.isArray(parsedResponse) && parsedResponse.every((ea) => {
        return typeof ea === "object" && Object.keys(ea).includes('id') && Object.keys(ea).includes('label');
      });
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
      if (!this.state.isTesting) {
        this.updateResult();
      }
    },

    onDone: function() {
      this.props.onDone();
    },

    updateResult: function() {
      this.setState({
        hasTested: true,
        isTesting: true
      }, this.fetchResult);
    },

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
          var newResults = this.state.results.concat(InvocationTestResult.fromReportJSON(json));
          this.setState({
            results: newResults,
            isTesting: false
          });
        },
        onError: () => {
          this.setState({
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
            <InvocationResults
              results={this.getResults()}
              resultStatus={this.renderResultStatus()}
              onRenderResult={this.renderResult}
            />
          </Collapsible>
          <div className="box-action">
            <div className="container phn">
              <div className="columns mtl">
                <div className="column column-page-sidebar">
                  <h4 className="type-weak">Test the data type</h4>
                </div>
                <div className="column column-three-quarters pll mobile-pln mobile-column-full">
                  {this.renderContent()}
                </div>
              </div>
            </div>
          </div>
        </div>
      );
    },

    renderContent: function() {
      var apps = this.props.appsRequiringAuth;
      if (apps.length > 0) {
        return (
          <TesterAuthRequired behaviorId={this.props.behaviorId} appsRequiringAuth={apps}/>
        );
      } else {
        return this.renderTester();
      }
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
      var result = this.getResults()[this.getResults().length - 1];
      if (this.state.isTesting) {
        return (
          <span className="type-weak pulse">Testing</span>
        );
      } else if (this.isValidResult(result)) {
        var numMatches = this.getParsedResponse(result).length;
        return (
          <span className="type-green">Last response valid {numMatches === 1 ? '(1 item)' : `(${numMatches} items)`} ✓</span>
        );
      } else if (result) {
        return (
          <span className="type-pink">
            <span>Last response invalid: must call <code>ellipsis.success</code> with an array of objects, </span>
            <span>each with an <code className="type-black">id</code> and <code className="type-black">label</code> property.</span>
          </span>
        );
      } else {
        return (
          <span className="type-weak">Last response missing</span>
        );
      }
    },

    renderResult: function(result) {
      var isValidResult = this.isValidResult(result);
      if (isValidResult) {
        return this.renderValidResultTableWith(this.getParsedResponse(result));
      } else {
        return (
          <div className="display-overflow-scroll border border-pink bg-white pas">
            {result.responseText ? (
              <pre>{result.responseText}</pre>
            ) : (
              <i>(No response occurred.)</i>
            )}
          </div>
        );
      }
    },

    renderValidResultTableWith: function(response) {
      var hasOtherData = response.some((item) => {
        return Object.keys(item).filter((key) => {
          return key !== 'id' && key !== 'label';
        }).length > 0;
      });
      var overflowResults = response.length - MAX_RESULTS_TO_SHOW;
      if (response.length > 0) {
        return (
          <div className="border pas border-green bg-white">
            <div className="columns columns-elastic">
              <div className="column-group">
                <div className="column-row type-s type-monospace">
                  <div className="column column-shrink pbxs">id</div>
                  <div className="column column-shrink pbxs">label</div>
                  <div className="column column-expand pbxs">
                    {hasOtherData ? "Other properties" : ""}
                  </div>
                </div>
                {response.slice(0, MAX_RESULTS_TO_SHOW).map((item, itemIndex) => (
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
          <div className="type-italic border pas border-green bg-white">An empty list was returned.</div>
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
                <button className="button-primary mbs" type="button"
                  onClick={this.onClick}
                  disabled={this.state.isTesting || (this.props.isSearch && !this.state.searchQuery)}
                >Test</button>
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
