define(function() {
  class Trigger {
    constructor(props) {
      Object.assign(this, {
        caseSensitive: false,
        isRegex: false,
        requiresMention: false,
        text: ""
      }, props);
    }

    get paramNames() {
      var matches = this.text.match(/\{.+?\}/g) || [];
      return matches.map((name) => name.replace(/^\{|\}$/g, ''));
    }

    hasNonRegexParams() {
      return !this.isRegex && /\{.+?\}/.test(this.text);
    }

    usesParamName(name) {
      var pattern = new RegExp(`\{${name}\}`);
      return !this.isRegex && pattern.test(this.text);
    }

    hasRegexCapturingParens() {
      return this.isRegex && /\(.+?\)/.test(this.text);
    }

    getTextWithNewParamName(oldName, newName) {
      var pattern = new RegExp(`\{${oldName}\}`, 'g');
      var wrappedNewName = `{${newName}}`;
      if (!this.isRegex) {
        return this.text.replace(pattern, wrappedNewName);
      } else {
        return this.text;
      }
    }

    clone(props) {
      return new Trigger(Object.assign({}, this, props));
    }
  }

  return Trigger;
});
