import * as React from 'react';
import * as debounce from 'javascript-debounce';
import DeepEqual from '../lib/deep_equal';
import autobind from "../lib/autobind";
import * as codemirror from 'codemirror';
import {Doc, Editor, EditorChangeLinkedList, EditorConfiguration, EditorFromTextArea} from "codemirror";

export type CodeMirrorOptions = EditorConfiguration & {
  hintOptions?: {
    hint: (cm: Doc) => void
  },
  autoCloseBrackets?: boolean,
  matchBrackets?: boolean
}

interface Props {
  onChange: (newValue: string) => void,
  onFocusChange?: (isFocused: boolean) => void,
  onScroll?: (scrollInfo: {}) => void,
  onViewportChange?: (cm: Editor, from: number, to: number) => void,
  onCursorChange: (cm: Editor) => void,
  options: CodeMirrorOptions,
  value: string,
  className?: Option<string>
}

interface State {
  isFocused: boolean
}

class CodeMirrorWrapper extends React.Component<Props, State> {
  textarea: Option<HTMLTextAreaElement>;
  editor: Option<EditorFromTextArea>;
  updateProps: (props: Props) => void;

  constructor(props: Props) {
    super(props);
    autobind(this);
    this.state = {
      isFocused: false
    };
  }

  componentDidMount(): void {
    if (this.textarea) {
      this.editor = codemirror.fromTextArea(this.textarea, this.props.options);
      this.editor.on('change', this.codemirrorValueChanged);
      this.editor.on('focus', this.onFocus);
      this.editor.on('blur', this.onBlur);
      this.editor.on('scroll', this.scrollChanged);
      this.editor.on('viewportChange', this.viewportChanged);
      this.editor.on('cursorActivity', this.cursorChanged);
      this.editor.setValue(this.props.value || '');
      this.updateProps = debounce((nextProps: Props) => {
        if (this.editor && this.editor.getValue() !== nextProps.value) {
          this.editor.setValue(nextProps.value);
        }
        for (let key in nextProps.options) {
          this.setCodeMirrorOptionIfChanged(key, nextProps.options[key]);
        }
      }, 0);
    }
  }
  componentWillUnmount(): void {
    // is there a lighter-weight way to remove the cm instance?
    if (this.editor) {
      this.editor.toTextArea();
    }
  }
  componentWillReceiveProps(nextProps: Props): void {
    this.updateProps(nextProps);
  }
  setCodeMirrorOptionIfChanged(optionName: string, newValue: any): void {
    if (this.editor) {
      const oldValue = this.editor.getOption(optionName);
      if (!DeepEqual.isEqual(oldValue, newValue)) {
        this.editor.setOption(optionName, newValue);
      }
    }
  }
  focus(): void {
    if (this.editor) {
      this.editor.focus();
    }
  }
  focusChanged(focused: boolean): void {
    this.setState({
      isFocused: focused
    });
    if (this.props.onFocusChange) {
      this.props.onFocusChange(focused);
    }
  }
  onFocus(): void {
    this.focusChanged(true);
  }
  onBlur(): void {
    this.focusChanged(false);
  }
  scrollChanged(cm: Editor): void {
    if (this.props.onScroll) {
      this.props.onScroll(cm.getScrollInfo());
    }
  }
  viewportChanged(cm: Editor, from: number, to: number): void {
    if (this.props.onViewportChange) {
      this.props.onViewportChange(cm, from, to);
    }
  }
  cursorChanged(cm: Editor): void {
    if (this.props.onCursorChange && this.state.isFocused) {
      this.props.onCursorChange(cm);
    }
  }
  codemirrorValueChanged(cm: Editor, change: EditorChangeLinkedList): void {
    if (this.props.onChange && change.origin !== 'setValue') {
      this.props.onChange(cm.getValue());
    }
  }

  shouldComponentUpdate(newProps: Props, newState: State): boolean {
    return (newProps.className !== this.props.className ||
      newState.isFocused !== this.state.isFocused);
  }

  render() {
    return (
      <div className={
        'ReactCodeMirror ' +
        (this.state.isFocused ? ' ReactCodeMirror--focused ' : '') +
        (this.props.className || '')
      }>
        <textarea
          ref={(el) => this.textarea = el}
          defaultValue={this.props.value}
          autoComplete='off'
        />
      </div>
    );
  }
}

export default CodeMirrorWrapper;
