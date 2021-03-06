import User, {UserJson} from './user';

export interface BehaviorGroupDeploymentJson {
  id: string;
  groupId: string;
  groupVersionId: string;
  deployer?: Option<UserJson>;
  createdAt: number;
}

interface BehaviorGroupDeploymentInterface extends BehaviorGroupDeploymentJson {
  deployer?: Option<User>
}

class BehaviorGroupDeployment implements BehaviorGroupDeploymentInterface {
  constructor(
    readonly id: string,
    readonly groupId: string,
    readonly groupVersionId: string,
    readonly deployer: Option<User>,
    readonly createdAt: number
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
        json.deployer ? User.fromJson(json.deployer) : null,
        json.createdAt
      );
    }

}

export default BehaviorGroupDeployment;

