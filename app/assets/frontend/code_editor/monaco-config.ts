// Enabled features:
import 'monaco-editor/esm/vs/editor/browser/controller/coreCommands';
import 'monaco-editor/esm/vs/editor/contrib/bracketMatching/bracketMatching';
import 'monaco-editor/esm/vs/editor/contrib/comment/comment';
import 'monaco-editor/esm/vs/editor/contrib/contextmenu/contextmenu';
import 'monaco-editor/esm/vs/editor/contrib/dnd/dnd';
import 'monaco-editor/esm/vs/editor/contrib/find/findController';
import 'monaco-editor/esm/vs/editor/contrib/folding/folding';
import 'monaco-editor/esm/vs/editor/contrib/goToDefinition/goToDefinitionCommands';
import 'monaco-editor/esm/vs/editor/contrib/goToDefinition/goToDefinitionMouse';
import 'monaco-editor/esm/vs/editor/contrib/hover/hover';
import 'monaco-editor/esm/vs/editor/contrib/multicursor/multicursor';
import 'monaco-editor/esm/vs/editor/contrib/parameterHints/parameterHints';
import 'monaco-editor/esm/vs/editor/contrib/rename/rename';
import 'monaco-editor/esm/vs/editor/contrib/suggest/suggestController';
import 'monaco-editor/esm/vs/editor/contrib/wordHighlighter/wordHighlighter';
import 'monaco-editor/esm/vs/editor/contrib/wordOperations/wordOperations';
import 'monaco-editor/esm/vs/editor/standalone/browser/quickOpen/quickCommand';

// Disabled features:

// linesOperations interferes with browser back/forward with default key bindings
// import 'monaco-editor/esm/vs/editor/contrib/linesOperations/linesOperations';

// import 'monaco-editor/esm/vs/editor/browser/widget/codeEditorWidget';
// import 'monaco-editor/esm/vs/editor/browser/widget/diffEditorWidget';
// import 'monaco-editor/esm/vs/editor/browser/widget/diffNavigator';
// import 'monaco-editor/esm/vs/editor/contrib/caretOperations/caretOperations';
// import 'monaco-editor/esm/vs/editor/contrib/caretOperations/transpose';
// import 'monaco-editor/esm/vs/editor/contrib/codeAction/codeAction';
// import 'monaco-editor/esm/vs/editor/contrib/clipboard/clipboard';
// import 'monaco-editor/esm/vs/editor/contrib/codelens/codelensController';
// import 'monaco-editor/esm/vs/editor/contrib/colorPicker/colorDetector';
// import 'monaco-editor/esm/vs/editor/contrib/cursorUndo/cursorUndo';
// import 'monaco-editor/esm/vs/editor/contrib/format/formatActions';
// import 'monaco-editor/esm/vs/editor/contrib/gotoError/gotoError';
// import 'monaco-editor/esm/vs/editor/contrib/inPlaceReplace/inPlaceReplace';
// import 'monaco-editor/esm/vs/editor/contrib/links/links';
// import 'monaco-editor/esm/vs/editor/contrib/referenceSearch/referenceSearch';
// import 'monaco-editor/esm/vs/editor/contrib/smartSelect/smartSelect';
// import 'monaco-editor/esm/vs/editor/contrib/snippet/snippetController2';
// import 'monaco-editor/esm/vs/editor/contrib/toggleTabFocusMode/toggleTabFocusMode';
// import 'monaco-editor/esm/vs/editor/standalone/browser/accessibilityHelp/accessibilityHelp';
// import 'monaco-editor/esm/vs/editor/standalone/browser/inspectTokens/inspectTokens';
// import 'monaco-editor/esm/vs/editor/standalone/browser/iPadShowKeyboard/iPadShowKeyboard';
// import 'monaco-editor/esm/vs/editor/standalone/browser/quickOpen/quickOutline';
// import 'monaco-editor/esm/vs/editor/standalone/browser/quickOpen/gotoLine';
// import 'monaco-editor/esm/vs/editor/standalone/browser/toggleHighContrast/toggleHighContrast';
// import * as monaco from 'monaco-editor/esm/vs/editor/editor.api';

// Enabled languages:
import 'monaco-editor/esm/vs/basic-languages/_.contribution';
import 'monaco-editor/esm/vs/language/typescript/monaco.contribution';
import 'monaco-editor/esm/vs/basic-languages/markdown/markdown';
import 'monaco-editor/esm/vs/basic-languages/markdown/markdown.contribution';
import 'monaco-editor/esm/vs/basic-languages/javascript/javascript';
import 'monaco-editor/esm/vs/basic-languages/typescript/typescript';
import 'monaco-editor/esm/vs/basic-languages/javascript/javascript.contribution';
import 'monaco-editor/esm/vs/basic-languages/typescript/typescript.contribution';
import 'monaco-editor/esm/vs/language/typescript/tsMode';

// Disabled languages:
// import 'monaco-editor/esm/vs/language/css/monaco.contribution';
// import 'monaco-editor/esm/vs/language/json/monaco.contribution';
// import 'monaco-editor/esm/vs/language/html/monaco.contribution';
// import 'monaco-editor/esm/vs/basic-languages/bat/bat.contribution';
// import 'monaco-editor/esm/vs/basic-languages/coffee/coffee.contribution';
// import 'monaco-editor/esm/vs/basic-languages/cpp/cpp.contribution';
// import 'monaco-editor/esm/vs/basic-languages/csharp/csharp.contribution';
// import 'monaco-editor/esm/vs/basic-languages/csp/csp.contribution';
// import 'monaco-editor/esm/vs/basic-languages/css/css.contribution';
// import 'monaco-editor/esm/vs/basic-languages/dockerfile/dockerfile.contribution';
// import 'monaco-editor/esm/vs/basic-languages/fsharp/fsharp.contribution';
// import 'monaco-editor/esm/vs/basic-languages/go/go.contribution';
// import 'monaco-editor/esm/vs/basic-languages/handlebars/handlebars.contribution';
// import 'monaco-editor/esm/vs/basic-languages/html/html.contribution';
// import 'monaco-editor/esm/vs/basic-languages/ini/ini.contribution';
// import 'monaco-editor/esm/vs/basic-languages/java/java.contribution';
// import 'monaco-editor/esm/vs/basic-languages/less/less.contribution';
// import 'monaco-editor/esm/vs/basic-languages/lua/lua.contribution';
// import 'monaco-editor/esm/vs/basic-languages/msdax/msdax.contribution';
// import 'monaco-editor/esm/vs/basic-languages/mysql/mysql.contribution';
// import 'monaco-editor/esm/vs/basic-languages/objective-c/objective-c.contribution';
// import 'monaco-editor/esm/vs/basic-languages/pgsql/pgsql.contribution';
// import 'monaco-editor/esm/vs/basic-languages/php/php.contribution';
// import 'monaco-editor/esm/vs/basic-languages/postiats/postiats.contribution';
// import 'monaco-editor/esm/vs/basic-languages/powershell/powershell.contribution';
// import 'monaco-editor/esm/vs/basic-languages/pug/pug.contribution';
// import 'monaco-editor/esm/vs/basic-languages/python/python.contribution';
// import 'monaco-editor/esm/vs/basic-languages/r/r.contribution';
// import 'monaco-editor/esm/vs/basic-languages/razor/razor.contribution';
// import 'monaco-editor/esm/vs/basic-languages/redis/redis.contribution';
// import 'monaco-editor/esm/vs/basic-languages/redshift/redshift.contribution';
// import 'monaco-editor/esm/vs/basic-languages/ruby/ruby.contribution';
// import 'monaco-editor/esm/vs/basic-languages/sb/sb.contribution';
// import 'monaco-editor/esm/vs/basic-languages/scss/scss.contribution';
// import 'monaco-editor/esm/vs/basic-languages/solidity/solidity.contribution';
// import 'monaco-editor/esm/vs/basic-languages/sql/sql.contribution';
// import 'monaco-editor/esm/vs/basic-languages/swift/swift.contribution';
// import 'monaco-editor/esm/vs/basic-languages/vb/vb.contribution';
// import 'monaco-editor/esm/vs/basic-languages/xml/xml.contribution';
// import 'monaco-editor/esm/vs/basic-languages/yaml/yaml.contribution';
