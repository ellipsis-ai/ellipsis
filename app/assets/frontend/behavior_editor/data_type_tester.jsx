import React from 'react';
import {testInvocation} from './behavior_test';
import Input from '../form/input';
import Collapsible from '../shared_ui/collapsible';
import {RequiredOAuth1Application} from '../models/oauth1';
import {RequiredOAuth2Application} from '../models/oauth2';
import TesterAuthRequired from './tester_auth_required';
import InvocationResults from './behavior_tester_invocation_results';
import InvocationTestResult from '../models/behavior_invocation_result';
import BehaviorTesterInvocationResultFile from './behavior_tester_invocation_result_file';

var MAX_RESULTS_TO_SHOW = 10;

const DataTypeTester = React.createClass({
    propTypes: {
      behaviorId: React.PropTypes.string,
      isSearch: React.PropTypes.bool,
      csrfToken: React.PropTypes.string.isRequired,
      onDone: React.PropTypes.func.isRequired,
      oauth1AppsRequiringAuth: React.PropTypes.arrayOf(React.PropTypes.instanceOf(RequiredOAuth1Application)).isRequired,
      oauth2AppsRequiringAuth: React.PropTypes.arrayOf(React.PropTypes.instanceOf(RequiredOAuth2Application)).isRequired
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
        const parsed = JSON.parse(result.responseText);
        if (parsed.every(ea => typeof ea === "string")) {
          return parsed.map(ea => {
            return { label: ea, id: ea };
          });
        } else {
          return parsed;
        }
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
      testInvocation({
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
                  <h4 className="mtn type-weak">Test the data type</h4>
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
      if (this.props.behaviorId && (this.props.oauth1AppsRequiringAuth.length > 0 || this.props.oauth2AppsRequiringAuth.length > 0)) {
        return (
          <TesterAuthRequired
            behaviorId={this.props.behaviorId}
            oauth1AppsRequiringAuth={this.props.oauth1AppsRequiringAuth}
            oauth2AppsRequiringAuth={this.props.oauth2AppsRequiringAuth}
          />
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
        return this.renderValidResultTableWith(result);
      } else {
        return (
          <div className="display-overflow-scroll border border-pink bg-white pas">
            {result.responseText ? (
              <pre>{result.responseText}</pre>
            ) : (
              <i>{"(No response occurred.)"}</i>
            )}
          </div>
        );
      }
    },

    renderFiles(result) {
      const files = result.files;
      if (files && files.length > 0) {
        return (
          <div>
            {files.map((file, index) => (
              <BehaviorTesterInvocationResultFile key={`file${index}`}
                                                  filename={file.filename}
                                                  filetype={file.filetype}
                                                  content={file.content}
              />
            ))}
          </div>
        );
      }
    },

    renderValidResultTableWith: function(result) {
      const response = this.getParsedResponse(result);
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
            {this.renderFiles(result)}
          </div>
        );
      } else {
        return (
          <div className="border pas border-green bg-white">
            <div className="type-italic">An empty list was returned.</div>
            {this.renderFiles(result)}
          </div>
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

export default DataTypeTester;
