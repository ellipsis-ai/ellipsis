import User, {UserJson} from './user';
import {Timestamp} from "../lib/formatter";

export interface BehaviorGroupMetaDataJson {
  groupId: string;
  initialCreatedAt: Timestamp;
  initialAuthor: UserJson | null;
}

class BehaviorGroupMetaData {
  groupId: string;
  initialCreatedAt: Timestamp;
  initialAuthor: User | null;

  constructor(groupId: string, initialCreatedAt: Timestamp, initialAuthor: User | null) {
    Object.defineProperties(this, {
      groupId: { value: groupId, enumerable: true },
      initialCreatedAt: { value: initialCreatedAt, enumerable: true },
      initialAuthor: { value: initialAuthor, enumerable: true }
    });
  }

  static fromJson(json: BehaviorGroupMetaDataJson) {
    return new BehaviorGroupMetaData(
      json.groupId,
      json.initialCreatedAt,
      json.initialAuthor ? User.fromJson(json.initialAuthor) : null
    );
  }
}

export default BehaviorGroupMetaData;
