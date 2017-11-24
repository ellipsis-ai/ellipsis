// @flow
define(function(require) {
  const User = require('./user');

  class BehaviorGroupVersionMetaData {
    behaviorGroupId: string;
    createdAt: number;
    author: ?User;

    constructor(
      behaviorGroupId: string,
      createdAt: number,
      author: ?User,
    ) {
      Object.defineProperties(this, {
        behaviorGroupId: { value: behaviorGroupId, enumerable: true },
        createdAt: { value: createdAt, enumerable: true },
        author: { value: author, enumerable: true }
      });
    }

    static fromProps(props) {
      return new BehaviorGroupVersionMetaData(
        props.behaviorGroupId,
        props.createdAt,
        props.author
      );
    }

    static fromJson(props) {
      return BehaviorGroupVersionMetaData.fromProps(Object.assign({}, props, {
        author: props.author ? User.fromJson(props.author) : null
      }));
    }
  }

  return BehaviorGroupVersionMetaData;
});
