// @flow
define(function(require) {
  const User = require('./user');

  class BehaviorGroupVersionMetaData {
    behaviorGroupId: string;
    createdAt: number;
    author: ?User;

    constructor(props) {
      const initialProps = Object.assign({}, props);
      Object.defineProperties(this, {
        behaviorGroupId: { value: initialProps.behaviorGroupId, enumerable: true },
        createdAt: { value: initialProps.createdAt, enumerable: true },
        author: { value: initialProps.author, enumerable: true }
      });
    }

    static fromJson(props) {
      return new BehaviorGroupVersionMetaData(Object.assign({}, props, {
        author: props.author ? User.fromJson(props.author) : null
      }));
    }
  }

  return BehaviorGroupVersionMetaData;
});
