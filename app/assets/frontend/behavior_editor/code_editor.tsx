import * as React from 'react';
import autobind from "../lib/autobind";
import MonacoEditor from "react-monaco-editor";
import {languages, editor, IDisposable} from "monaco-editor";
import {lib_es5_dts, lib_es2015_dts} from "monaco-editor/esm/vs/language/typescript/lib/lib";
import {NODE_JS_V6_D_TS} from "../code_editor/definitions/nodejs";
import 'monaco-editor/esm/vs/basic-languages/javascript/javascript';
import 'monaco-editor/esm/vs/basic-languages/typescript/typescript';
import 'monaco-editor/esm/vs/basic-languages/markdown/markdown';
import 'monaco-editor/esm/vs/language/typescript/tsMode';
import typescript = languages.typescript;
import javascriptDefaults = languages.typescript.javascriptDefaults;

/* Monaco loads as a global instance, so we only want to set defaults once on page load: */
javascriptDefaults.setCompilerOptions({
  target: typescript.ScriptTarget.ES2015,
  module: typescript.ModuleKind.ES2015,
  lib: ["es5", "es2015"],
  allowNonTsExtensions: true,
  allowJs: true,
  checkJs: true,
  moduleResolution: typescript.ModuleResolutionKind.NodeJs,
  noImplicitAny: false,
  strictFunctionTypes: true,
  strictNullChecks: true
});
javascriptDefaults.setDiagnosticsOptions({
  noSemanticValidation: false,
  noSyntaxValidation: false
});
javascriptDefaults.addExtraLib(lib_es5_dts, `es5-${Date.now()}`);
javascriptDefaults.addExtraLib(lib_es2015_dts, `es2015-${Date.now()}`);
javascriptDefaults.addExtraLib(NODE_JS_V6_D_TS, `node_js_v6-${Date.now()}`);

export interface EditorCursorPosition {
  top: number
  bottom: number
}

interface Props {
  availableHeight: number
  firstLineNumber: number
  lineWrapping?: boolean
  onChange: (newValue: string) => void
  onCursorChange: (newPosition: EditorCursorPosition) => void
  value: string
  definitions: string
  language: "javascript" | "typescript" | "markdown"
  monacoOptions?: editor.IEditorConstructionOptions
}

const MIN_EDITOR_LINES = 12;
const EDITOR_LINE_HEIGHT = 24;
const EDITOR_FONT_SIZE = 15;

class CodeEditor extends React.Component<Props> {
  constructor(props: Props) {
    super(props);
    autobind(this);
  }

  editor: Option<editor.IStandaloneCodeEditor>;
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
    this.currentDefinitions = javascriptDefaults.addExtraLib(newDefinitions, `ellipsis-${Date.now()}`);
  }

  editorDidMount(editor: editor.IStandaloneCodeEditor): void {
    this.editor = editor;
    editor.onDidChangeCursorPosition(this.onEditorPositionChange);
    const tabSize = this.tabSizeForLanguage();
    if (tabSize) {
      editor.getModel().updateOptions({
        tabSize: tabSize
      });
    }
  }

  onEditorPositionChange(event: editor.ICursorPositionChangedEvent): void {
    if (this.container && this.editor) {
      const scrolledPosition = this.editor.getScrolledVisiblePosition(event.position);
      const rect = this.container.getBoundingClientRect();
      const top = rect.top + window.scrollY + scrolledPosition.top;
      this.props.onCursorChange({
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

  getMonacoOptions(): editor.IEditorConstructionOptions {
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
      scrollBeyondLastLine: false,
    }, this.props.monacoOptions);
  }

  tabSizeForLanguage(): Option<number> {
    if (this.props.language === "markdown") {
      return 4;
    } else if (this.props.language === "javascript" || this.props.language === "typescript") {
      return 2;
    } else {
      return;
    }
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
