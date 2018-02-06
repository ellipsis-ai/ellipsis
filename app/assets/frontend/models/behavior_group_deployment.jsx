// @flow
import User from './user';

class BehaviorGroupDeployment {
    id: string;
    groupId: string;
    groupVersionId: string;
    deployer: ?User;
    createdAt: number;

    constructor(
      id: string,
      groupId: string,
      groupVersionId: string,
      deployer: ?User,
      createdAt: number
    ) {
      Object.defineProperties(this, {
        id: { value: id, enumerable: true },
        groupId: { value: groupId, enumerable: true },
        groupVersionId: { value: groupVersionId, enumerable: true },
        deployer: { value: deployer, enumerable: true },
        createdAt: { value: createdAt, enumerable: true }
      });
    }

    static fromProps(props): BehaviorGroupDeployment {
      return new BehaviorGroupDeployment(
        props.id,
        props.groupId,
        props.groupVersionId,
        User.fromProps(props.deployer),
        props.createdAt
      );
    }

}

export default BehaviorGroupDeployment;

