define(function(require) {
var React = require('react'),
  debounce = require('javascript-debounce');

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

return React.createClass({
  displayName: 'CodeMirror',

  propTypes: {
    onChange: React.PropTypes.func,
    onFocusChange: React.PropTypes.func,
    onScroll: React.PropTypes.func,
    onViewportChange: React.PropTypes.func,
    onCursorChange: React.PropTypes.func,
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
    var textareaNode = this.refs.textarea;
    var codeMirrorInstance = this.getCodeMirrorInstance();
    this.codeMirror = codeMirrorInstance.fromTextArea(textareaNode, this.props.options);
    this.codeMirror.on('change', this.codemirrorValueChanged);
    this.codeMirror.on('focus', this.focusChanged.bind(this, true));
    this.codeMirror.on('blur', this.focusChanged.bind(this, false));
    this.codeMirror.on('scroll', this.scrollChanged);
    this.codeMirror.on('viewportChange', this.viewportChanged);
    this.codeMirror.on('cursorActivity', this.cursorChanged);
    this.codeMirror.setValue(this.props.value || '');
    this.updateProps = debounce(function (nextProps) {
      if (this.codeMirror && nextProps.value !== undefined && this.codeMirror.getValue() !== nextProps.value) {
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
    if (this.props.onFocusChange) {
      this.props.onFocusChange(focused);
    }
  },
  scrollChanged: function scrollChanged(cm) {
    if (this.props.onScroll) {
      this.props.onScroll(cm.getScrollInfo());
    }
  },
  viewportChanged: function viewportChanged(cm, from, to) {
    if (this.props.onViewportChange) {
      this.props.onViewportChange(cm, from, to);
    }
  },
  cursorChanged: function cursorChanged(cm) {
    if (this.props.onCursorChange && this.state.isFocused) {
      this.props.onCursorChange(cm);
    }
  },
  codemirrorValueChanged: function codemirrorValueChanged(doc, change) {
    if (this.props.onChange && change.origin !== 'setValue') {
      this.props.onChange(doc.getValue());
    }
  },
  render: function render() {
    return (
      <div className={
        'ReactCodeMirror ' +
        (this.state.isFocused ? ' ReactCodeMirror--focused ' : '') +
        (this.props.className || '')
      }>
        <textarea
          ref="textarea"
          name={this.props.path}
          defaultValue={this.props.value}
          autoComplete='off'
        />
      </div>
    );
  }
});

});
