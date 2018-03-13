import {Diffable, DiffableProp} from './diffs';

import ParamType, {ParamTypeJson} from './param_type';

export interface InputJson {
  name: string;
  question: string;
  paramType: ParamTypeJson;
  isSavedForTeam: boolean;
  isSavedForUser: boolean;
  inputId: string;
  inputVersionId: string;
  exportId: string;
}

interface InputInterface extends InputJson {
  paramType: ParamType;
}

class Input implements Diffable, InputInterface {
  constructor(
    readonly name: string,
    readonly question: string,
    readonly paramType: ParamType,
    readonly isSavedForTeam: boolean,
    readonly isSavedForUser: boolean,
    readonly inputId: string,
    readonly inputVersionId: string,
    readonly exportId: string
  ) {

      Object.defineProperties(this, {
        name: {
          value: name,
          enumerable: true
        },
        question: {
          value: question,
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

    static fromProps(props: InputInterface) {
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

    static fromJson(json: InputJson) {
      return Input.fromProps(Object.assign({}, json, {
        paramType: ParamType.fromJson(json.paramType)
      }));
    }

    static allFromJson(jsonArray: Array<InputJson>) {
      return jsonArray.map((triggerObj) => Input.fromJson(triggerObj));
    }
}

export default Input;
