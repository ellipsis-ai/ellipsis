import User, {UserJson} from './user';

export interface BehaviorGroupVersionMetaDataJson {
  behaviorGroupId: string,
  createdAt: number,
  author?: Option<UserJson>
}

interface BehaviorGroupVersionMetaDataInterface extends BehaviorGroupVersionMetaDataJson {
  author?: Option<User>
}

class BehaviorGroupVersionMetaData implements BehaviorGroupVersionMetaDataInterface {
  constructor(
    readonly behaviorGroupId: string,
    readonly createdAt: number,
    readonly author: Option<User>
  ) {
      Object.defineProperties(this, {
        behaviorGroupId: { value: behaviorGroupId, enumerable: true },
        createdAt: { value: createdAt, enumerable: true },
        author: { value: author, enumerable: true }
      });
  }

    static fromProps(props: BehaviorGroupVersionMetaDataInterface): BehaviorGroupVersionMetaData {
      return new BehaviorGroupVersionMetaData(
        props.behaviorGroupId,
        props.createdAt,
        props.author
      );
    }

    static fromJson(props: BehaviorGroupVersionMetaDataJson): BehaviorGroupVersionMetaData {
      return BehaviorGroupVersionMetaData.fromProps(Object.assign({}, props, {
        author: props.author ? User.fromJson(props.author) : null
      }));
    }
}

export default BehaviorGroupVersionMetaData;

