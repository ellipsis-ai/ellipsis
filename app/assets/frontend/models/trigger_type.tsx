import {Diffable, DiffableProp} from "./diffs";

export interface TriggerTypeJson {
  id: string,
  displayString: string
}

interface TriggerTypeInterface extends TriggerTypeJson {}

class TriggerType implements Diffable, TriggerTypeInterface {
  readonly id: string;
  readonly displayString: string;

  constructor(
    id: string,
    displayString: string
  ) {
    Object.defineProperties(this, {
      id: {
        value: id,
        enumerable: true
      },
      displayString: {
        value: displayString,
        enumerable: true
      }
    });
  }

  diffLabel(): string {
    const itemLabel = this.itemLabel();
    const kindLabel = this.kindLabel();
    return itemLabel ? `${kindLabel} “${itemLabel}”` : `empty ${kindLabel}`;
  }

  itemLabel(): Option<string> {
    return this.displayString;
  }

  kindLabel(): string {
    return "trigger type";
  }

  getIdForDiff(): string {
    return this.id;
  }

  diffProps(): Array<DiffableProp> {
    return [{
      name: "Trigger type",
      value: this.displayString
    }];
  }

  clone(props: Partial<TriggerTypeInterface>): TriggerType {
    return TriggerType.fromProps(Object.assign({}, this, props));
  }

  static fromProps(props: TriggerTypeInterface): TriggerType {
    return new TriggerType(
      props.id,
      props.displayString
    );
  }

}

export default TriggerType;

