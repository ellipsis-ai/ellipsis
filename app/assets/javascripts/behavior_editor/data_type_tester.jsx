define(function(require) {
  var React = require('react'),
    debounce = require('javascript-debounce');
  require('whatwg-fetch');

  return React.createClass({
    displayName: 'DataTypeTester',
    propTypes: {
      behaviorId: React.PropTypes.string.isRequired,
      csrfToken: React.PropTypes.string.isRequired,
      onDone: React.PropTypes.func.isRequired
    },

    getInitialState: function() {
      return {
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
      formData.append('paramValuesJson', '[]');
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

    renderTester: function() {
      return (
        <div>

          <div className="mvxl">
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
    },

    renderTrigger: function(trigger, index) {
      var highlighted = this.state.highlightedTriggerText === trigger.text;
      var className = "pvs border-bottom " +
        (trigger.isRegex ? " type-monospace " : "") +
        (highlighted ? " type-bold type-green " : "");
      return (
        <div ref={`trigger${index}`} key={`trigger${index}`} className={className}>
          {trigger.text} {highlighted ? "✓" : ""}
        </div>
      );
    },

    renderNoTriggers: function() {
      return (
        <div>
          <p>This behavior does not have any triggers. Add at least one trigger before testing.</p>

          <div className="mvxl">
            <button type="button" onClick={this.props.onDone}>OK</button>
          </div>
        </div>
      );
    },

    renderParams: function(params) {
      return (
        <div className="columns columns-elastic">
          <div className="column-group">
            {params.map((param, index) => (
              <div key={`param${index}`} className="column-row">
                <div className="column column-shrink type-monospace type-weak prs pvxs">{param.name}:</div>
                <div className="column column-expand pvxs">{this.getValueForParamName(param.name)}</div>
              </div>
            ))}
          </div>
        </div>
      );
    },

    renderNoParams: function() {
      return (
        <p>No user input has been defined.</p>
      );
    }
  });
});
