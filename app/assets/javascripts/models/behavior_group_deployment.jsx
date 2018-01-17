// @flow

define(function() {
  class BehaviorGroupDeployment {
    id: string;
    groupId: string;
    groupVersionId: string;
    createdAt: number;

    constructor(
      id: string,
      groupId: string,
      groupVersionId: string,
      createdAt: number
    ) {
      Object.defineProperties(this, {
        id: { value: id, enumerable: true },
        groupId: { value: groupId, enumerable: true },
        groupVersionId: { value: groupVersionId, enumerable: true },
        createdAt: { value: createdAt, enumerable: true }
      });
    }

    static fromProps(props): BehaviorGroupDeployment {
      return new BehaviorGroupDeployment(
        props.id,
        props.groupId,
        props.groupVersionId,
        props.createdAt
      );
    }

  }

  return BehaviorGroupDeployment;
});
