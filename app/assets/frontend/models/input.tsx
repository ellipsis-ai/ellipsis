import {Diffable, DiffableProp} from './diffs';

import ParamType, {ParamTypeJson} from './param_type';

export interface InputJson {
  id?: Option<String>;
  name: string;
  question: string;
  paramType?: Option<ParamTypeJson>;
  isSavedForTeam: boolean;
  isSavedForUser: boolean;
  inputId?: Option<string>;
  exportId?: Option<string>;
}

interface InputInterface extends InputJson {
  paramType?: Option<ParamType>
}

class Input implements Diffable, InputInterface {
  constructor(
    readonly name: string,
    readonly question: string,
    readonly paramType: Option<ParamType>,
    readonly isSavedForTeam: boolean,
    readonly isSavedForUser: boolean,
    readonly inputId: Option<string>,
    readonly exportId: Option<string>
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

    itemLabel(): Option<string> {
      return this.name;
    }

    kindLabel(): string {
      return "input";
    }

    getIdForDiff(): string {
      return this.exportId || "unknown";
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
        value: this.paramType ? this.paramType.name : "",
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
      return Boolean(this.name === other.name &&
        this.paramType && other.paramType &&
        this.paramType.id === other.paramType.id);
    }

    clone(props: Partial<InputInterface>): Input {
      return Input.fromProps(Object.assign({}, this, props));
    }

    static fromProps(props: InputInterface): Input {
      return new Input(
        props.name,
        props.question,
        props.paramType,
        props.isSavedForTeam,
        props.isSavedForUser,
        props.inputId,
        props.exportId
      );
    }

    static fromJson(json: InputJson): Input {
      return Input.fromProps(Object.assign({}, json, {
        paramType: json.paramType ? ParamType.fromJson(json.paramType) : null
      }));
    }

    static allFromJson(jsonArray: Array<InputJson>) {
      return jsonArray.map((triggerObj) => Input.fromJson(triggerObj));
    }
}

export default Input;
