define(function() {
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

    replace(pattern, newString) {
      var newText = this.text.replace(pattern, newString);
      if (newText !== this.text) {
        return this.clone({
          text: newText
        });
      } else {
        return this;
      }
    }

    static fromString(string) {
      return new ResponseTemplate({ text: string });
    }
  }

  return ResponseTemplate;

});


