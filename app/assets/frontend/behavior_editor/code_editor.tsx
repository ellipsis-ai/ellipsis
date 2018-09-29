import * as React from 'react';
import MonacoEditor from "react-monaco-editor";
import 'monaco-editor/esm/vs/basic-languages/javascript/javascript';
import 'monaco-editor/esm/vs/basic-languages/typescript/typescript';
import 'monaco-editor/esm/vs/language/typescript/tsMode';
import * as monacoEditor from "monaco-editor";
import {editor} from "monaco-editor";
import IStandaloneCodeEditor = editor.IStandaloneCodeEditor;
import autobind from "../lib/autobind";

interface Props {
  firstLineNumber: number
  functionParams: Array<string>
  lineWrapping?: boolean
  onChange: (newValue: string) => void
  onCursorChange: () => void
  value: string
  definitions: string
}

class CodeEditor extends React.Component<Props> {
  constructor(props) {
    super(props);
    autobind(this);
  }

  editor: Option<IStandaloneCodeEditor>;

  editorWillMount(monaco: typeof monacoEditor): void {
    monaco.languages.typescript.typescriptDefaults.setCompilerOptions({
      target: monaco.languages.typescript.ScriptTarget.ES2015,
      module: monaco.languages.typescript.ModuleKind.ES2015,
      lib: ["ES2015"],
      allowNonTsExtensions: true,
      strictNullChecks: true
    });
    monaco.languages.typescript.typescriptDefaults.addExtraLib(this.props.definitions);
  }

  editorDidMount(editor: IStandaloneCodeEditor): void {
    this.editor = editor;
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
      <div className="position-relative" style={{ height: this.getEditorHeight() }}>
        <MonacoEditor
          language="typescript"
          value={this.props.value}
          onChange={this.props.onChange}
          options={{
            automaticLayout: true,
            fontSize: 15,
            lineHeight: 24,
            fontFamily: "Source Code Pro",
            lineNumbers: (lineNumber) => String(lineNumber + this.props.firstLineNumber - 1),
            wordWrap: this.props.lineWrapping ? "on" : "off"
          }}
          editorWillMount={this.editorWillMount}
          editorDidMount={this.editorDidMount}
        />
      </div>
    );
  }
}

export default CodeEditor;
