// @flow
define(function(require) {

  const ParamType = require('./param_type');

  class Input {
    name: string;
    question: string;
    paramType: ParamType;
    isSavedForTeam: boolean;
    isSavedForUser: boolean;
    inputId: string;
    inputVersionId: string;
    exportId: string;

    constructor(
      name: ?string,
      question: ?string,
      paramType: string,
      isSavedForTeam: boolean,
      isSavedForUser: boolean,
      inputId: string,
      inputVersionId: string,
      exportId: string
    ) {

      Object.defineProperties(this, {
        name: {
          value: name || '',
          enumerable: true
        },
        question: {
          value: question || '',
          enumerable: true
        },
        paramType: {
          value: paramType,
          enumerable: true
        },
        isSavedForTeam: {
          value: isSavedForTeam,
          enumerable: true
        },
        isSavedForUser: {
          value: isSavedForUser,
          enumerable: true
        },
        inputId: {
          value: inputId,
          enumerable: true
        },
        inputVersionId: {
          value: inputVersionId,
          enumerable: true
        },
        exportId: {
          value: exportId,
          enumerable: true
        }
      });
    }

    isSaved(): boolean {
      return this.isSavedForUser || this.isSavedForTeam;
    }

    isSameNameAndTypeAs(other: Input): boolean {
      return this.name === other.name &&
        this.paramType.id === other.paramType.id;
    }

    clone(props) {
      return Input.fromProps(Object.assign({}, this, props));
    }

    static fromProps(props) {
      return new Input(
        props.name,
        props.question,
        props.paramType,
        props.isSavedForTeam,
        props.isSavedForUser,
        props.inputId,
        props.inputVersionId,
        props.exportId
      );
    }

    static allFromJson(jsonArray) {
      return jsonArray.map((triggerObj) => Input.fromProps(triggerObj));
    }
  }

  return Input;
});
