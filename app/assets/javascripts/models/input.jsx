// @flow

import type Diffable, {Diff} from './diffs';

define(function(require) {

  const DeepEqual = require('../lib/deep_equal');
  const diffs = require('./diffs');
  const ParamType = require('./param_type');

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
      name: ?string,
      question: ?string,
      paramType: string,
      isSavedForTeam: ?boolean,
      isSavedForUser: ?boolean,
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

    forEqualityComparison() {
      return this;
    }

    isIdenticalToVersion(version): boolean {
      return DeepEqual.isEqual(this.forEqualityComparison(), version.forEqualityComparison());
    }

    diffLabel(): string {
      return `input "${this.name}"`;
    }

    getIdForDiff(): string {
      return this.inputId;
    }

    maybeDiffFor(other: Input): ?diffs.ModifiedDiff<Input> {
      if (this.isIdenticalToVersion(other)) {
        return null;
      } else {
        const children: Array<Diff> = [
          diffs.TextPropertyDiff.maybeFor("Name", this.name, other.name),
          diffs.TextPropertyDiff.maybeFor("Question", this.question, other.question),
          diffs.TextPropertyDiff.maybeFor("Type", this.paramType.name, other.paramType.name),
          diffs.BooleanPropertyDiff.maybeFor("Saved for whole team", this.isSavedForTeam, other.isSavedForTeam),
          diffs.BooleanPropertyDiff.maybeFor("Saved per user", this.isSavedForUser, other.isSavedForUser)
        ].filter(ea => Boolean(ea));
        return new diffs.ModifiedDiff(children, this, other);
      }
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
