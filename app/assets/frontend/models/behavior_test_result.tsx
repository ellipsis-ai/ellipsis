export interface BehaviorTestResultJson {
  behaviorVersionId: string,
  isPass: boolean,
  output: string,
  runAt: number
}

interface BehaviorTestResultInterface extends BehaviorTestResultJson {}

class BehaviorTestResult implements BehaviorTestResultInterface {
  constructor(
    readonly behaviorVersionId: string,
    readonly isPass: boolean,
    readonly output: string,
    readonly runAt: number
  ) {
    Object.defineProperties(this, {
      behaviorVersionId: {value: behaviorVersionId, enumerable: true},
      isPass: {value: isPass, enumerable: true},
      output: {value: output, enumerable: true},
      runAt: {value: runAt, enumerable: true}
    });
  }

  clone(props: Partial<BehaviorTestResultInterface>): BehaviorTestResult {
    return BehaviorTestResult.fromProps(Object.assign({}, this, props));
  }

  static fromProps(props: BehaviorTestResultInterface): BehaviorTestResult {
    return new BehaviorTestResult(props.behaviorVersionId, props.isPass, props.output, props.runAt);
  }

  static allFromJson(jsonArray: Array<BehaviorTestResultJson>) {
    return jsonArray.map((props) => BehaviorTestResult.fromProps(props));
  }

}

export default BehaviorTestResult;

