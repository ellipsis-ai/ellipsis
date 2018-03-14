import User, {UserJson} from './user';
import {Timestamp} from "../lib/formatter";

export interface BehaviorGroupMetaDataJson {
  groupId: string;
  initialCreatedAt: Timestamp;
  initialAuthor: UserJson | null;
}

interface BehaviorGroupMetaDataInterface extends BehaviorGroupMetaDataJson {
  initialAuthor: User | null
}

class BehaviorGroupMetaData implements BehaviorGroupMetaDataInterface {
  constructor(
    readonly groupId: string,
    readonly initialCreatedAt: Timestamp,
    readonly initialAuthor: User | null
  ) {
    Object.defineProperties(this, {
      groupId: { value: groupId, enumerable: true },
      initialCreatedAt: { value: initialCreatedAt, enumerable: true },
      initialAuthor: { value: initialAuthor, enumerable: true }
    });
  }

  static fromJson(json: BehaviorGroupMetaDataJson): BehaviorGroupMetaData {
    return new BehaviorGroupMetaData(
      json.groupId,
      json.initialCreatedAt,
      json.initialAuthor ? User.fromJson(json.initialAuthor) : null
    );
  }
}

export default BehaviorGroupMetaData;
