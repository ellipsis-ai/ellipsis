define(function(require) {
  var React = require('react'),
    Checklist = require('./checklist');

  return React.createClass({
    propTypes: {
      functionBody: React.PropTypes.string.isRequired,
      usesSearch: React.PropTypes.bool.isRequired
    },

    hasCalledOnSuccess: function() {
      var code = this.props.functionBody;
      return /\bonSuccess\([\s\S]*?\)/.test(code) ||
        /\bellipsis\.success\([\s\S]*?\)/.test(code);
    },

    hasCalledRequire: function() {
      var code = this.props.functionBody;
      return /\brequire\(\s*\S.+?\)/.test(code);
    },

    hasCode: function() {
      return /\S/.test(this.props.functionBody);
    },

    codeUsesSearchQuery: function() {
      var code = this.props.functionBody;
      return /searchQuery/.test(code);
    },

    render: function() {
      return (
        <div>
          <Checklist disabledWhen={false}>
            <Checklist.Item hiddenWhen={this.hasCode()}>
              <span>Write a node.js function. You can <code>require()</code> any </span>
              <span><a href="https://www.npmjs.com/" target="_blank">NPM package</a>.</span>
            </Checklist.Item>

            <Checklist.Item hiddenWhen={!this.hasCode() || this.hasCalledRequire()}>
              <span>Use <code>require(…)</code> to load any NPM package.</span>
            </Checklist.Item>

            <Checklist.Item checkedWhen={this.hasCalledOnSuccess()} hiddenWhen={false}>
              <span>End the function by calling </span>
              <code className="type-bold">ellipsis.success(<span className="type-regular">…</span>)</code>
              <span> with an array of objects, each with <code className="type-bold">id</code> and <code className="type-bold">label</code> properties.</span>
              <pre className="box-code-example"><code>
{`ellipsis.success([
  { label: 'one', id: '1' },
  { label: 'two', id: '2' },
  { label: 'three', id: '3' }
]);`}
              </code></pre>
            </Checklist.Item>

            <Checklist.Item checkedWhen={this.codeUsesSearchQuery()} hiddenWhen={!this.props.usesSearch}>
              <span>You can use the <code className="type-bold">searchQuery</code> parameter to help filter the results</span>
            </Checklist.Item>

          </Checklist>
        </div>
      );
    }
  });
});
