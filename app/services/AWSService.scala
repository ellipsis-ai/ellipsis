package services

import com.amazonaws.auth.BasicAWSCredentials
import play.api.Configuration

trait AWSService {

  val credentials = new BasicAWSCredentials(configuration.getString("aws.accessKey").getOrElse("foo"), configuration.getString("aws.secretKey").getOrElse("foo"))

  val configuration: Configuration
}
