define(function() {
  class Trigger {
    constructor(props) {
      var initialProps = Object.assign({
        caseSensitive: false,
        isRegex: false,
        requiresMention: false,
        text: ""
      }, props);
      Object.defineProperties(this, {
        caseSensitive: {
          value: initialProps.caseSensitive,
          enumerable: true
        },
        isRegex: {
          value: initialProps.isRegex,
          enumerable: true
        },
        requiresMention: {
          value: initialProps.requiresMention,
          enumerable: true
        },
        text: {
          value: initialProps.text,
          enumerable: true
        }
      });
    }

    get paramNames() {
      var names = [];
      var matches = this.text.match(/\{.+?\}/g);
      if (!this.isRegex && matches) {
        names = matches.map((name) => name.replace(/^\{|\}$/g, ''));
      }
      return names;
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

    static triggersFromJson(jsonArray) {
      return jsonArray.map((triggerObj) => new Trigger(triggerObj));
    }
  }

  return Trigger;
});
