define(function() {
  var validTemplateKeywordPatterns = [
    /^for\s+\S+\s+in\s+.+$/,
    /^endfor$/,
    /^if\s+.+$/,
    /^endif$/
  ];

  function stringStartsWithVarName(string, varName) {
    return string.split('.')[0].trim() === varName;
  }

  class ResponseTemplate {
    constructor(props) {
      var initialProps = Object.assign({
        text: ""
      }, props);
      Object.defineProperties(this, {
        text: {
          value: initialProps.text,
          enumerable: true
        }
      });
    }

    clone(props) {
      return new ResponseTemplate(Object.assign({}, this, props));
    }

    toString() {
      return this.text;
    }

    toJSON() {
      return this.toString();
    }

    getParamsUsed() {
      var matches = this.text.match(/\{.+?\}/g);
      return matches ? matches.map((ea) => ea.replace(/^\{\s*|\s*\}$/g, '')) : [];
    }

    getVarsDefinedInTemplateLoops() {
      var matches = this.text.match(/\{for\s+\S+\s+in\s+.+\}/g);
      return matches ? matches.map((ea) => ea.replace(/^\{for\s+|\s+in\s+.+\}$/g, '')) : [];
    }

    getUnknownParamsExcluding(validParams) {
      var varsDefinedInForLoops = this.getVarsDefinedInTemplateLoops();
      return this.getParamsUsed().filter((param) => {
        return !validParams.some((validParam) => stringStartsWithVarName(param, validParam)) &&
          !varsDefinedInForLoops.some((varName) => stringStartsWithVarName(param, varName)) &&
          !validTemplateKeywordPatterns.some((pattern) => pattern.test(param));
      });
    }

    replaceParamName(oldName, newName) {
      var newText = this.text.split(`{${oldName}}`).join(`{${newName}}`);
      if (newText !== this.text) {
        return this.clone({
          text: newText
        });
      } else {
        return this;
      }
    }

    includesAnyParam() {
      return /\{\S+?\}/.test(this.text);
    }

    includesIteration() {
      return /\{endfor\}/.test(this.text);
    }

    includesPath() {
      return /\{(\S+\.\S+)+?\}/.test(this.text);
    }

    includesSuccessResult() {
      return /\{successResult.*?\}/.test(this.text);
    }

    usesMarkdown() {
      /* Big ugly flaming pile of regex to try and guess at Markdown usage: */
      var matches = [
        '\\*.+?\\*', /* Bold/italics */
        '_.+?_', /* Bold/italics */
        '\\[.+?\\]\\(.+?\\)', /* Links */
        '(\\[.+?\\]){2}', /* Links by reference */
        '^.+\\n[=-]+', /* Underlined headers */
        '^#+\\s+.+', /* # Headers */
        '^\\d\\.\\s+.+', /* Numbered lists */
        '^\\*\\s+.+', /* Bulleted lists */
        '^>.+', /* Block quote */
        '`.+?`', /* Code */
        '```', /* Code block */
        '^\\s*[-\\*]\\s*[-\\*]\\s*[-\\*]' /* Horizontal rule */
      ];
      var matchRegExp = new RegExp( '(' + matches.join( ')|(' ) + ')' );
      return matchRegExp.test(this.text);
    }

    static fromString(string) {
      return new ResponseTemplate({ text: string });
    }
  }

  return ResponseTemplate;

});


