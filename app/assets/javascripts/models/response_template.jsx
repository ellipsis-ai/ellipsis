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

    static fromString(string) {
      return new ResponseTemplate({ text: string });
    }
  }

  return ResponseTemplate;

});


