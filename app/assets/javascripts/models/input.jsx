define(function() {

  class Input {
    constructor(props) {
      var initialProps = Object.assign({
        name: '',
        question: '',
        paramType: null,
        isSavedForTeam: false,
        isSavedForUser: false,
        inputId: null,
        inputVersionId: null,
        exportId: null
      }, props);

      if (!initialProps.inputId) {
        throw new Error("New Input must have an inputId property");
      }
      if (!initialProps.paramType) {
        throw new Error("New Input object must have a param type set");
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
        },
        isSavedForTeam: {
          value: initialProps.isSavedForTeam,
          enumerable: true
        },
        isSavedForUser: {
          value: initialProps.isSavedForUser,
          enumerable: true
        },
        inputId: {
          value: initialProps.inputId,
          enumerable: true
        },
        inputVersionId: {
          value: initialProps.inputVersionId,
          enumerable: true
        },
        exportId: {
          value: initialProps.exportId,
          enumerable: true
        }
      });
    }

    isSaved() {
      return this.isSavedForUser || this.isSavedForTeam;
    }

    isSameNameAndTypeAs(other) {
      return this.name === other.name &&
        this.paramType.id === other.paramType.id;
    }

    clone(props) {
      return new Input(Object.assign({}, this, props));
    }

    static allFromJson(jsonArray) {
      return jsonArray.map((triggerObj) => new Input(triggerObj));
    }
  }

  return Input;
});
