define(function() {
  class Trigger {
    constructor(props) {
      var initialProps = Object.assign({
        caseSensitive: false,
        isRegex: false,
        requiresMention: true,
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

    get displayText() {
      return this.requiresMention ? `...${this.text}` : this.text;
    }

    hasNonRegexParams() {
      return !this.isRegex && /\{.+?\}/.test(this.text);
    }

    usesParamName(name) {
      return !this.isRegex && this.text.includes(`{${name}}`);
    }

    capturesParamIndex(index) {
      if (!this.isRegex) {
        return false;
      }
      var matches = this.text.match(/^\(.+?\)|[^\\]\(.*?[^\\]\)/g);
      return !!(matches && matches[index]);
    }

    hasRegexCapturingParens() {
      return this.isRegex && /\(.+?\)/.test(this.text);
    }

    hasCaseInsensitiveRegexFlagWhileCaseSensitive() {
      return this.isRegex && this.caseSensitive && /^\(\?i\)/.test(this.text);
    }

    getTextWithNewParamName(oldName, newName) {
      if (!this.isRegex) {
        return this.text.split(`{${oldName}}`).join(`{${newName}}`);
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
