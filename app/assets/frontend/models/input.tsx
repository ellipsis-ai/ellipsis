import {Diffable, DiffableProp} from './diffs';

import ParamType from './param_type';

class Input implements Diffable {
    name: string;
    question: string;
    paramType: ParamType;
    isSavedForTeam: boolean;
    isSavedForUser: boolean;
    inputId: string;
    inputVersionId: string;
    exportId: string;

    constructor(
      name: string | null,
      question: string | null,
      paramType: ParamType,
      isSavedForTeam: boolean | null,
      isSavedForUser: boolean | null,
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
          value: !!isSavedForTeam,
          enumerable: true
        },
        isSavedForUser: {
          value: !!isSavedForUser,
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

    diffLabel(): string {
      const name = this.itemLabel();
      const kindLabel = this.kindLabel();
      return name ? `${kindLabel} “${name}”` : `unnamed ${kindLabel}`;
    }

    itemLabel(): string | null {
      return this.name;
    }

    kindLabel(): string {
      return "input";
    }

    getIdForDiff(): string {
      return this.exportId;
    }

    diffProps(): Array<DiffableProp> {
      return [{
        name: "Name",
        value: this.name
      }, {
        name: "Question",
        value: this.question
      }, {
        name: "Data type",
        value: this.paramType.name,
        isCategorical: true
      }, {
        name: "Save and re-use answer for the team",
        value: this.isSavedForTeam
      }, {
        name: "Save and re-use answer for each user",
        value: this.isSavedForUser
      }];
    }

    isSaved(): boolean {
      return this.isSavedForUser || this.isSavedForTeam;
    }

    isSameNameAndTypeAs(other: Input): boolean {
      return this.name === other.name &&
        this.paramType.id === other.paramType.id;
    }

    clone(props: {}) {
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

export default Input;
