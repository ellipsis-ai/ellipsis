define(function() {
  class Param {
    constructor(props) {
      var initialProps = Object.assign({
        name: '',
        question: '',
        paramType: null
      }, props);

      if (!initialProps.paramType) {
        throw(new Error("New Param object must have a param type set"));
      }
      Object.defineProperties(this, {
        name: {
          value: initialProps.name,
          enumerable: true
        },
        question: {
          value: initialProps.question,
          enumerable: true
        },
        paramType: {
          value: initialProps.paramType,
          enumerable: true
        }
      });
    }

    clone(props) {
      return new Param(Object.assign({}, this, props));
    }

    static paramsFromJason(jsonArray) {
      return jsonArray.map((triggerObj) => new Param(triggerObj));
    }
  }

  return Param;
});
