// @flow
define(function(require) {
  type Timestamp = string | number | Date

  const moment = require('moment');
  const ONE_WEEK_IN_MS = 1000 * 60 * 60 * 24 * 7;

  const RESERVED_WORDS = Object.freeze([
    // ECMAScript 2015 reserved words:
    'break', 'case', 'catch', 'class', 'const', 'continue', 'debugger', 'default',
    'delete', 'do', 'else', 'export', 'extends', 'finally', 'for', 'function',
    'if', 'import', 'in', 'instanceof', 'new', 'return', 'super', 'switch',
    'this', 'throw', 'try', 'typeof', 'var', 'void', 'while', 'with', 'yield',

    // ECMAScript future reserved words:
    'enum', 'implements', 'interface', 'let', 'package', 'private', 'protected',
    'public', 'static',

    // Module reserved words:
    'await',

    // Old future reserved words:
    'abstract', 'boolean', 'byte', 'char', 'double', 'final', 'float', 'goto',
    'int', 'long', 'native', 'short', 'synchronized', 'throws', 'transient',
    'volatile',

    // Reserved literals:
    'null', 'true', 'false'
  ]);

  function matchesReservedWord(name: string): boolean {
    return RESERVED_WORDS.some((word) => word === name);
  }

  function wasProbablyReservedWord(name: string): boolean {
    return RESERVED_WORDS.some((word) => name.indexOf('_' + word) === 0 && name.length === word.length + 2);
  }

  const Formatter = {
    formatTimestampLong: function(timestamp: Timestamp): string {
      const m = moment(timestamp);
      return `${m.format('dddd, LL')} at ${m.format('LT')}`;
    },

    formatTimestampRelative: function(timestamp: Timestamp): string {
      return moment(timestamp).fromNow();
    },

    formatTimestampRelativeIfRecent: function(timestamp: Timestamp): string {
      var then = typeof timestamp === "number" ? timestamp : new Date(timestamp).valueOf();
      var now = Date.now();
      var diff = now - then > 0 ? now - then : then - now;
      if (diff < ONE_WEEK_IN_MS) {
        return this.formatTimestampRelative(timestamp);
      } else {
        return this.formatTimestampShort(timestamp);
      }
    },

    formatTimestampShort: function(timestamp: Timestamp): string {
      return moment(timestamp).format('ll, LT');
    },

    formatTimestampDate: function(timestamp: Timestamp): string {
      return moment(timestamp).format('ll');
    },

    formatTimestampTime: function(timestamp: Timestamp): string {
      return moment(timestamp).format('LT');
    },

    formatCamelCaseIdentifier: function(phrase: string): string {
      var split = phrase.split(' ').map((ea) => ea.replace(/[^\w$]/ig, ''));
      var firstWord = split[0].charAt(0).toLowerCase() + split[0].slice(1);
      var camel = firstWord + split.slice(1).map((ea) => ea.charAt(0).toUpperCase() + ea.slice(1)).join("");
      if (/^[^a-z_$]/i.test(phrase.charAt(0))) {
        camel = "_" + camel;
      }
      return camel;
    },

    formatEnvironmentVariableName: function(name: string): string {
      return name.toUpperCase().replace(/\s/g, '_').replace(/^\d|[^A-Z0-9_]/g, '');
    },

    formatNameForCode: function(name: string): string {
      // Needs to satisfy requirements for GraphQL identifiers, which is a subset of valid JavaScript identifiers:
      // Regex: /[_A-Za-z][_0-9A-Za-z]*/
      const validForJS = name.replace(/[^_0-9A-Za-z]/g, '').replace(/^[^_A-Za-z]/, '');

      // GraphQL field names also can't begin with two underscores, per the spec https://facebook.github.io/graphql/#sec-Objects
      const validForGraphQL = validForJS.replace(/^__*/, '_');

      if (matchesReservedWord(validForGraphQL)) {
        return '_' + validForGraphQL;
      } else if (wasProbablyReservedWord(validForGraphQL)) {
        return validForGraphQL.replace(/^_/, '');
      } else {
        return validForGraphQL;
      }
    },

    formatDataTypeName: function(name: string): string {
      const forCode = this.formatNameForCode(name);
      const startingWithLetter = forCode.replace(/^[^a-zA-Z]*/, '');
      const capitalized = startingWithLetter.charAt(0).toUpperCase() + startingWithLetter.slice(1);
      return capitalized;
    },

    isValidNameForCode: function(name: string): boolean {
      return Formatter.formatNameForCode(name) === name;
    },

    formatList: function<T>(list: Array<T>, optionalMapper?: (T) => string): string {
      var mapper = optionalMapper || ((ea) => ea);
      if (list.length === 1) {
        return list.map(mapper).join("");
      } else if (list.length === 2) {
        return list.map(mapper).join(" and ");
      } else if (list.length > 2) {
        return list.slice(0, list.length - 1).map(mapper).join(", ") +
            ", and " + list.slice(list.length - 1).map(mapper).join("");
      } else {
        return "";
      }
    },

    formatPossibleNumber: function(value: string): string {
      const minus = value.charAt(0) === "-" ? "-" : "";
      const stripped = value
        .replace(/[^\d.]/g, "")
        .replace(/^0+/, "0")
        .replace(/^0+([1-9])/, "$1");
      const pieces = stripped.split(".");
      const beforeDecimal = pieces[0];
      const afterDecimal = pieces.length > 1 ? "." + pieces.slice(1).join("") : "";
      return minus + beforeDecimal + afterDecimal;
    },

    formatGitBranchIdentifier: function(value: string): string {
      // See https://git-scm.com/docs/git-check-ref-format
      //
      // We don't worry about trailing values since a user might keep typing
      return value
        .replace(/\.+/g, ".") // no double dots
        .replace(/\/+/g, "/") // no double slashes
        .replace(/[~^:?*[\\\s]/g, "") // no ASCII control characters, backslashes, spaces, etc
        .replace(/@{/g, "") // no @{
        .replace(/^@$/, "") // no single @
        .replace(/^[.\/]+/, ""); // no leading dots or slashes
    },

    formatGithubRepoName: function(value: string): string {
      return value.replace(/[^a-z0-9\-_.]/gi, "");
    },

    formatGithubUserName: function(value: string): string {
      // Github's join page says:
      //  Username may only contain alphanumeric characters or single hyphens,
      //  and cannot begin or end with a hyphen
      //
      // However, we cannot strip trailing hyphens since a user might keep typing
      return value
        .replace(/[^a-z0-9\-]/gi, "")
        .replace(/-+/, "-")
        .replace(/^-+/, "");
    }
  };

  return Formatter;
});
