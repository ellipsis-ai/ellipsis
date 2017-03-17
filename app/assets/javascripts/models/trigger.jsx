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

    getText() {
      return this.text || "";
    }

    get paramNames() {
      var names = [];
      var matches = this.getText().match(/\{.+?\}/g);
      if (!this.isRegex && matches) {
        names = matches.map((name) => name.replace(/^\{|\}$/g, ''));
      }
      return names;
    }

    get displayText() {
      return this.getText();
    }

    hasNonRegexParams() {
      return !this.isRegex && /\{.+?\}/.test(this.getText());
    }

    usesInputName(name) {
      return !this.isRegex && this.getText().includes(`{${name}}`);
    }

    capturesInputIndex(index) {
      if (!this.isRegex) {
        return false;
      }
      var matches = this.getText().match(/^\(.+?\)|[^\\]\(.*?[^\\]\)/g);
      return !!(matches && matches[index]);
    }

    hasRegexCapturingParens() {
      return this.isRegex && /\(.+?\)/.test(this.getText());
    }

    hasCaseInsensitiveRegexFlagWhileCaseSensitive() {
      return this.isRegex && this.caseSensitive && /^\(\?i\)/.test(this.getText());
    }

    getTextWithNewInputName(oldName, newName) {
      if (!this.isRegex) {
        return this.getText().split(`{${oldName}}`).join(`{${newName}}`);
      } else {
        return this.getText();
      }
    }

    clone(props) {
      return new Trigger(Object.assign({}, this, props));
    }

    static triggersFromJson(jsonArrayOrNull) {
      if (jsonArrayOrNull) {
        return jsonArrayOrNull.map((triggerObj) => new Trigger(triggerObj));
      } else {
        return null;
      }
    }
  }

  return Trigger;
});
