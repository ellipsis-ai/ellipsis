import * as React from 'react';
import autobind from "../lib/autobind";
import MonacoEditor from "react-monaco-editor";
import * as monacoEditor from "monaco-editor";
import {editor, IScrollEvent} from "monaco-editor";
import IStandaloneCodeEditor = editor.IStandaloneCodeEditor;
import {lib_es5_dts} from "monaco-editor/esm/vs/language/typescript/lib/lib";
import {NODE_JS_V6_D_TS} from "../code_editor/definitions/nodejs";
import 'monaco-editor/esm/vs/basic-languages/javascript/javascript';
import 'monaco-editor/esm/vs/basic-languages/typescript/typescript';
import 'monaco-editor/esm/vs/language/typescript/tsMode';

export interface EditorScrollPosition {
  top: number
  bottom: number
}

interface Props {
  firstLineNumber: number
  lineWrapping?: boolean
  onChange: (newValue: string) => void
  onScrollChange: (newPosition: EditorScrollPosition) => void
  value: string
  definitions: string
}

class CodeEditor extends React.Component<Props> {
  constructor(props: Props) {
    super(props);
    autobind(this);
  }

  editor: Option<IStandaloneCodeEditor>;
  container: Option<HTMLDivElement>;

  wordWrapOptionFor(lineWrapping?: boolean) {
    return lineWrapping ? "on" : "off";
  }

  componentWillReceiveProps(newProps: Props): void {
    if (newProps.lineWrapping !== this.props.lineWrapping && this.editor) {
      this.editor.updateOptions({
        wordWrap: this.wordWrapOptionFor(newProps.lineWrapping)
      })
    }
  }

  editorWillMount(monaco: typeof monacoEditor): void {
    const defaults = monaco.languages.typescript.javascriptDefaults;
    defaults.setCompilerOptions({
      target: monaco.languages.typescript.ScriptTarget.ES2015,
      module: monaco.languages.typescript.ModuleKind.ES2015,
      lib: ["es5", "es2015"],
      allowNonTsExtensions: true,
      allowJs: true,
      checkJs: true,
      moduleResolution: monaco.languages.typescript.ModuleResolutionKind.NodeJs,
      noImplicitAny: false,
      strictFunctionTypes: true,
      strictNullChecks: true
    });
    defaults.setDiagnosticsOptions({
      noSemanticValidation: false,
      noSyntaxValidation: false
    });
    defaults.addExtraLib(lib_es5_dts, "es5");
    defaults.addExtraLib(NODE_JS_V6_D_TS, "node_js_v6");
    defaults.addExtraLib(this.props.definitions, "ellipsis");
  }

  editorDidMount(editor: IStandaloneCodeEditor): void {
    this.editor = editor;
    editor.onDidChangeCursorPosition(this.onEditorPositionChange)
  }

  onEditorPositionChange(cursorPosition): void {
    if (this.container && this.editor) {
      const scrolledPosition = this.editor.getScrolledVisiblePosition(cursorPosition);
      const rect = this.container.getBoundingClientRect();
      const top = rect.top + window.scrollY + scrolledPosition.top;
      this.props.onScrollChange({
        top: top,
        bottom: top + scrolledPosition.height
      });
    }
  }

  getEditorHeight(): string {
    const lines = Math.max(this.props.value.split("\n").length, 12);
    const lineHeight = 24;
    const availableHeight = window.innerHeight;
    const height = Math.min(lines * lineHeight, availableHeight);
    return `${height}px`;
  }

  render() {
    return (
      <div ref={(el) => this.container = el} className="position-relative" style={{ height: this.getEditorHeight() }}>
        <MonacoEditor
          language="javascript"
          value={this.props.value}
          onChange={this.props.onChange}
          options={{
            automaticLayout: true,
            fontSize: 15,
            lineHeight: 24,
            fontFamily: "Source Code Pro",
            lineNumbers: (lineNumber) => String(lineNumber + this.props.firstLineNumber - 1),
            wordWrap: this.wordWrapOptionFor(this.props.lineWrapping)
          }}
          editorWillMount={this.editorWillMount}
          editorDidMount={this.editorDidMount}
        />
      </div>
    );
  }
}

export default CodeEditor;
