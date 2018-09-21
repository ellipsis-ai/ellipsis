export interface BehaviorResponseTypeJson {
  id: string,
  displayString: string
}

interface BehaviorResponseTypeInterface extends BehaviorResponseTypeJson {}

class BehaviorResponseType implements BehaviorResponseTypeInterface {
  constructor(
    readonly id: string,
    readonly displayString: string
  ) {
    Object.defineProperties(this, {
      id: {value: id, enumerable: true},
      displayString: {value: displayString, enumerable: true}
    });
  }

  clone(props: Partial<BehaviorResponseTypeInterface>): BehaviorResponseType {
    return BehaviorResponseType.fromProps(Object.assign({}, this, props));
  }

  static fromProps(props: BehaviorResponseTypeInterface): BehaviorResponseType {
    return new BehaviorResponseType(props.id, props.displayString);
  }

}

export default BehaviorResponseType;

