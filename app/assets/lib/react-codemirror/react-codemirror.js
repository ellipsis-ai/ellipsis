define('react-codemirror', function(require, exports, module) {

'use strict';

var React = require('react');
var className = require('classnames');
var debounce = require('lodash.debounce');

function deepEquals(thing1, thing2) {
	if (thing1 === thing2) {
		return true;
	} else if (Number.isNaN(thing1) && Number.isNaN(thing2)) {
		return true;
	} else if (Array.isArray(thing1) && Array.isArray(thing2)) {
		return arraysEqual(thing1, thing2);
	} else if (typeof(thing1) === 'object' && typeof(thing2) === 'object') {
		return objectsEqual(thing1, thing2);
	} else {
		return false;
	}
}

function arraysEqual(array1, array2) {
	if (array1.length !== array2.length) {
		return false;
	} else {
		return array1.every(function(item, index) {
			return deepEquals(array1[index], array2[index]);
		});
	}
}

function objectsEqual(obj1, obj2) {
	if (obj1.constructor !== obj2.constructor) {
		return false;
	}
	var obj1Keys = Object.keys(obj1);
	var obj2Keys = Object.keys(obj2);
	if (!arraysEqual(obj1Keys.sort(), obj2Keys.sort())) {
		return false;
	}
	return obj1Keys.every(function(key) {
		return deepEquals(obj1[key], obj2[key]);
	});
}

var CodeMirror = React.createClass({
	displayName: 'CodeMirror',

	propTypes: {
		onChange: React.PropTypes.func,
		onFocusChange: React.PropTypes.func,
		onScroll: React.PropTypes.func,
		options: React.PropTypes.object,
		path: React.PropTypes.string,
		value: React.PropTypes.string,
		className: React.PropTypes.any,
		codeMirrorInstance: React.PropTypes.object
	},
	getCodeMirrorInstance: function getCodeMirrorInstance() {
		return this.props.codeMirrorInstance || require('codemirror');
	},
	getInitialState: function getInitialState() {
		return {
			isFocused: false
		};
	},
	componentDidMount: function componentDidMount() {
		var textareaNode = this.textarea;
		var codeMirrorInstance = this.getCodeMirrorInstance();
		this.codeMirror = codeMirrorInstance.fromTextArea(textareaNode, this.props.options);
		this.codeMirror.on('change', this.codemirrorValueChanged);
		this.codeMirror.on('focus', this.focusChanged.bind(this, true));
		this.codeMirror.on('blur', this.focusChanged.bind(this, false));
		this.codeMirror.on('scroll', this.scrollChanged);
		this.codeMirror.setValue(this.props.defaultValue || this.props.value || '');
		this.updateProps = debounce(function (nextProps) {
			if (this.codeMirror && nextProps.value !== undefined && this.codeMirror.getValue() != nextProps.value) {
				this.codeMirror.setValue(nextProps.value);
			}
			if (typeof nextProps.options === 'object') {
				for (var optionName in nextProps.options) {
					if (nextProps.options.hasOwnProperty(optionName)) {
						this.setCodeMirrorOptionIfChanged(optionName, nextProps.options[optionName]);
					}
				}
			}
		}, 0);
	},
	componentWillUnmount: function componentWillUnmount() {
		// is there a lighter-weight way to remove the cm instance?
		if (this.codeMirror) {
			this.codeMirror.toTextArea();
		}
	},
	componentWillReceiveProps: function componentWillReceiveProps(nextProps) {
		this.updateProps(nextProps);
	},
	getCodeMirror: function getCodeMirror() {
		return this.codeMirror;
	},
	setCodeMirrorOptionIfChanged: function(optionName, newValue) {
		var oldValue = this.codeMirror.getOption(optionName);
		if (!deepEquals(oldValue, newValue)) {
			this.codeMirror.setOption(optionName, newValue);
		}
	},
	focus: function focus() {
		if (this.codeMirror) {
			this.codeMirror.focus();
		}
	},
	focusChanged: function focusChanged(focused) {
		this.setState({
			isFocused: focused
		});
		this.props.onFocusChange && this.props.onFocusChange(focused);
	},
	scrollChanged: function scrollChanged(cm) {
		this.props.onScroll && this.props.onScroll(cm.getScrollInfo());
	},
	codemirrorValueChanged: function codemirrorValueChanged(doc, change) {
		if (this.props.onChange && change.origin != 'setValue') {
			this.props.onChange(doc.getValue());
		}
	},
	render: function render() {
		var _this = this;

		var editorClassName = className('ReactCodeMirror', this.state.isFocused ? 'ReactCodeMirror--focused' : null, this.props.className);
		return React.createElement(
			'div',
			{ className: editorClassName },
			React.createElement('textarea', { ref: function (node) {
					return _this.textarea = node;
				}, name: this.props.path, defaultValue: this.props.value, autoComplete: 'off' })
		);
	}
});

module.exports = CodeMirror;

});
