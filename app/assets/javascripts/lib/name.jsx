define(function() {

  const reservedWords = Object.freeze([
    'break', 'case', 'catch', 'class', 'const', 'continue', 'debugger', 'default',
    'delete', 'do', 'else', 'export', 'extends', 'finally', 'for', 'function',
    'if', 'import', 'in', 'instanceof', 'new', 'return', 'super', 'switch',
    'this', 'throw', 'try', 'typeof', 'var', 'void', 'while', 'with', 'yield'
  ]);

  function matchesReservedWord(name) {
    return reservedWords.some((word) => word === name);
  }

  function wasProbablyReservedWord(name) {
    return reservedWords.some((word) => name.indexOf('_' + word) === 0 && name.length > word.length + 1);
  }

  return {
    formatForCode(string) {
      // Needs to satisfy requirements for GraphQL identifiers, which is a subset of valid JavaScript identifiers:
      // Regex: /[_A-Za-z][_0-9A-Za-z]*/
      const validForJS = string.replace(/[^_0-9A-Za-z]/g, '').replace(/^[^_A-Za-z]/, '');

      // GraphQL field names also can't begin with two underscores, per the spec https://facebook.github.io/graphql/#sec-Objects
      const validForGraphQL = validForJS.replace(/^__*/, '_');

      if (matchesReservedWord(validForGraphQL)) {
        return '_' + validForGraphQL;
      } else if (wasProbablyReservedWord(validForGraphQL)) {
        return validForGraphQL.replace(/^_/, '');
      } else {
        return validForGraphQL;
      }
    }
  };
});
