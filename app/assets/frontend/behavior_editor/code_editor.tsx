import * as React from 'react';
import autobind from "../lib/autobind";
import MonacoEditor from "react-monaco-editor";
import * as monacoEditor from "monaco-editor";
import {editor, IDisposable} from "monaco-editor";
import IStandaloneCodeEditor = editor.IStandaloneCodeEditor;
import {lib_es5_dts} from "monaco-editor/esm/vs/language/typescript/lib/lib";
import {NODE_JS_V6_D_TS} from "../code_editor/definitions/nodejs";
import 'monaco-editor/esm/vs/basic-languages/javascript/javascript';
import 'monaco-editor/esm/vs/basic-languages/typescript/typescript';
import 'monaco-editor/esm/vs/basic-languages/markdown/markdown';
import 'monaco-editor/esm/vs/language/typescript/tsMode';
import {min} from "moment";

/* Monaco loads as a global instance, so we only want to set defaults once on page load: */
const defaults = monacoEditor.languages.typescript.javascriptDefaults;
defaults.setCompilerOptions({
  target: monacoEditor.languages.typescript.ScriptTarget.ES2015,
  module: monacoEditor.languages.typescript.ModuleKind.ES2015,
  lib: ["es5", "es2015"],
  allowNonTsExtensions: true,
  allowJs: true,
  checkJs: true,
  moduleResolution: monacoEditor.languages.typescript.ModuleResolutionKind.NodeJs,
  noImplicitAny: false,
  strictFunctionTypes: true,
  strictNullChecks: true
});
defaults.setDiagnosticsOptions({
  noSemanticValidation: false,
  noSyntaxValidation: false
});
defaults.addExtraLib(lib_es5_dts, `es5-${Date.now()}`);
defaults.addExtraLib(NODE_JS_V6_D_TS, `node_js_v6-${Date.now()}`);

export interface EditorScrollPosition {
  top: number
  bottom: number
}

interface Props {
  availableHeight: number
  firstLineNumber: number
  lineWrapping?: boolean
  onChange: (newValue: string) => void
  onScrollChange: (newPosition: EditorScrollPosition) => void
  value: string
  definitions: string
  language: string
  monacoOptions?: monacoEditor.editor.IEditorConstructionOptions
}

const MIN_EDITOR_LINES = 12;
const EDITOR_LINE_HEIGHT = 24;
const EDITOR_FONT_SIZE = 15;

class CodeEditor extends React.Component<Props> {
  constructor(props: Props) {
    super(props);
    autobind(this);
  }

  editor: Option<IStandaloneCodeEditor>;
  container: Option<HTMLDivElement>;
  currentDefinitions: Option<IDisposable>;

  wordWrapOptionFor(lineWrapping?: boolean) {
    return lineWrapping ? "on" : "off";
  }

  componentWillReceiveProps(newProps: Props): void {
    if (newProps.lineWrapping !== this.props.lineWrapping && this.editor) {
      this.editor.updateOptions({
        wordWrap: this.wordWrapOptionFor(newProps.lineWrapping)
      })
    }

    if (newProps.definitions !== this.props.definitions) {
      this.resetCurrentDefinitions(newProps.definitions);
    }
  }

  editorWillMount(): void {
    this.resetCurrentDefinitions(this.props.definitions);
  }

  componentWillUnmount(): void {
    if (this.currentDefinitions) {
      this.currentDefinitions.dispose();
    }
  }

  resetCurrentDefinitions(newDefinitions: string) {
    if (this.currentDefinitions) {
      this.currentDefinitions.dispose();
    }
    this.currentDefinitions = defaults.addExtraLib(newDefinitions, `ellipsis-${Date.now()}`);
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
    const minHeight = MIN_EDITOR_LINES * EDITOR_LINE_HEIGHT;
    const heightForContent = this.props.value.split("\n").length * EDITOR_LINE_HEIGHT;
    const maxHeightDesired = Math.min(heightForContent, this.props.availableHeight);
    const height = Math.max(maxHeightDesired, minHeight);
    return `${height}px`;
  }

  getMonacoOptions(): monacoEditor.editor.IEditorConstructionOptions {
    return Object.assign({
      automaticLayout: true,
      fontSize: EDITOR_FONT_SIZE,
      lineHeight: EDITOR_LINE_HEIGHT,
      fontFamily: "Source Code Pro",
      lineNumbers: (lineNumber) => String(lineNumber + this.props.firstLineNumber - 1),
      minimap: {
        enabled: false
      },
      wordWrap: this.wordWrapOptionFor(this.props.lineWrapping),
      scrollBeyondLastLine: false
    }, this.props.monacoOptions);
  }

  render() {
    return (
      <div ref={(el) => this.container = el} className="position-relative" style={{ height: this.getEditorHeight() }}>
        <MonacoEditor
          language={this.props.language}
          value={this.props.value}
          onChange={this.props.onChange}
          options={this.getMonacoOptions()}
          editorWillMount={this.editorWillMount}
          editorDidMount={this.editorDidMount}
        />
      </div>
    );
  }
}

export default CodeEditor;
