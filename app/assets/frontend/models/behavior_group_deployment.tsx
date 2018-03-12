import User, {UserJson} from './user';

export interface BehaviorGroupDeploymentJson {
  id: string;
  groupId: string;
  groupVersionId: string;
  deployer: UserJson | null;
  createdAt: number;
}

class BehaviorGroupDeployment {
    id: string;
    groupId: string;
    groupVersionId: string;
    deployer: User | null;
    createdAt: number;

    constructor(
      id: string,
      groupId: string,
      groupVersionId: string,
      deployer: User | null,
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

    static fromJson(json: BehaviorGroupDeploymentJson): BehaviorGroupDeployment {
      return new BehaviorGroupDeployment(
        json.id,
        json.groupId,
        json.groupVersionId,
        User.fromJson(json.deployer),
        json.createdAt
      );
    }

}

export default BehaviorGroupDeployment;

