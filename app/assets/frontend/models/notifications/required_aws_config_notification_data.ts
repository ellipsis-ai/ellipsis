import NotificationData, {NotificationDataInterface} from "../notification_data";
import {AWSConfigRef, RequiredAWSConfig} from "../aws";

interface RequiredAwsConfigDataInterface extends NotificationDataInterface {
  name: string;
  existingAWSConfigs: Array<AWSConfigRef>;
  requiredAWSConfig: RequiredAWSConfig;
  onUpdateAWSConfig: (config: RequiredAWSConfig, callback?: () => void) => void;
  onNewAWSConfig: (config: RequiredAWSConfig) => void;
  onConfigClick: (config: RequiredAWSConfig) => void;
}

class RequiredAwsConfigNotificationData extends NotificationData implements RequiredAwsConfigDataInterface {
  readonly name: string;
  readonly existingAWSConfigs: Array<AWSConfigRef>;
  readonly requiredAWSConfig: RequiredAWSConfig;
  readonly onUpdateAWSConfig: (config: RequiredAWSConfig, callback?: () => void) => void;
  readonly onNewAWSConfig: (config: RequiredAWSConfig) => void;
  readonly onConfigClick: (config: RequiredAWSConfig) => void;
  constructor(props: RequiredAwsConfigDataInterface) {
    super(props, "required_aws_config_without_config");
  }
}

export default RequiredAwsConfigNotificationData;
