import * as React from 'react';
import MonacoEditor from "react-monaco-editor";
import 'monaco-editor/esm/vs/basic-languages/javascript/javascript';
import 'monaco-editor/esm/vs/basic-languages/typescript/typescript';
import 'monaco-editor/esm/vs/language/typescript/tsMode';
import * as monacoEditor from "monaco-editor";

interface Props {
  firstLineNumber: number
  functionParams: Array<string>
  lineWrapping?: boolean
  onChange: (newValue: string) => void
  onCursorChange: () => void
  value: string
  autocompletions: Array<string>
}

class CodeEditor extends React.Component<Props> {
  autocompleteParams(cm: any) {
    const matches: Array<string> = [];
    const possibleWords = this.props.autocompletions;

    const cursor = cm.getCursor();
    const line = cm.getLine(cursor.line);
    let start = cursor.ch;
    let end = cursor.ch;

    while (start && /\w/.test(line.charAt(start - 1))) {
      --start;
    }
    while (end < line.length && /\w/.test(line.charAt(end))) {
      ++end;
    }

    const word = line.slice(start, end).toLowerCase();

    possibleWords.forEach(function(w) {
      const lowercase = w.toLowerCase();
      if (lowercase.indexOf(word) !== -1) {
        matches.push(w);
      }
    });

    return {
      list: matches,
      from: { line: cursor.line, ch: start },
      to: { line: cursor.line, ch: end }
    };
  }

  replaceTabsWithSpaces(cm: any) {
    var spaces = Array(cm.getOption("indentUnit") + 1).join(" ");
    const doc = cm.getDoc();
    doc.replaceSelection(spaces);
  }

  editorWillMount(monaco: typeof monacoEditor): void {
    monaco.languages.typescript.typescriptDefaults.setCompilerOptions({
      target: monaco.languages.typescript.ScriptTarget.ES2015,
      module: monaco.languages.typescript.ModuleKind.ES2015,
      lib: ["ES2015"],
      allowNonTsExtensions: true,
      strictNullChecks: true
    });
    monaco.languages.typescript.typescriptDefaults.addExtraLib([
`
declare function require(path: string): any;

type EllipsisSuccessOptions = {
  next?: {
    actionName: string
  }
}

declare var ellipsis = {
  success: (successResult: any, options?: EllipsisSuccessOptions) => void
};
`
    ].join('\n'));
  }

  render() {
    return (
      <div className="border" style={{ height: "300px" }}>
        <MonacoEditor
          language="typescript"
          value={this.props.value}
          onChange={this.props.onChange}
          options={{
            automaticLayout: true,
            fontSize: 15,
            fontFamily: "Source Code Pro",
            lineNumbers: (lineNumber) => String(lineNumber + this.props.firstLineNumber - 1),
            wordWrap: this.props.lineWrapping ? "on" : "off"
          }}
          editorWillMount={this.editorWillMount}
        />
      </div>
    );
  }
}

export default CodeEditor;
