package services

import com.amazonaws.auth.BasicAWSCredentials
import play.api.Configuration

trait AWSService {

  val configuration: Configuration

  val credentials = new BasicAWSCredentials(configuration.getString("aws.accessKey").getOrElse("foo"), configuration.getString("aws.secretKey").getOrElse("foo"))

}
