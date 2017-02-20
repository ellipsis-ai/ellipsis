define(function() {

  var reservedWords = Object.freeze([
    'break', 'case', 'catch', 'class', 'const', 'continue', 'debugger', 'default',
    'delete', 'do', 'else', 'export', 'extends', 'finally', 'for', 'function',
    'if', 'import', 'in', 'instanceof', 'new', 'return', 'super', 'switch',
    'this', 'throw', 'try', 'typeof', 'var', 'void', 'while', 'with', 'yield'
  ]);

  class Param {
    constructor(props) {
      var initialProps = Object.assign({
        name: '',
        question: '',
        paramType: null,
        isSavedForTeam: false,
        isSavedForUser: false,
        inputId: null,
        inputExportId: null
      }, props);

      // TODO: We can re-enable this once all published skills have params with param types
      // if (!initialProps.paramType) {
      //   throw(new Error("New Param object must have a param type set"));
      // }
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
        inputExportId: {
          value: initialProps.inputExportId,
          enumerable: true
        }
      });
    }

    isSaved() {
      return this.isSavedForUser || this.isSavedForTeam;
    }

    isSameNameAndTypeAs(otherParam) {
      return this.name === otherParam.name &&
        this.paramType.id === otherParam.paramType.id;
    }

    clone(props) {
      return new Param(Object.assign({}, this, props));
    }

    static paramsFromJson(jsonArray) {
      return jsonArray.map((triggerObj) => new Param(triggerObj));
    }

    static formatName(proposedName) {
      // This only allows a subset of valid JS identifiers, but it'll do for now
      // for our purposes
      var newName = proposedName.replace(/[^\w$]/g, '').replace(/^[^a-z$_]/i, '');
      if (Param.matchesReservedWord(newName)) {
        newName = '_' + newName;
      }
      return newName;
    }

    static isValidName(proposedName) {
      var formattedName = Param.formatName(proposedName);
      return formattedName === proposedName;
    }

    static matchesReservedWord(name) {
      return reservedWords.some((word) => word === name);
    }

    static get reservedWords() {
      return reservedWords;
    }
  }

  return Param;
});
