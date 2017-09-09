package services

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.regions.Regions
import play.api.Configuration

trait AWSService {

  val configuration: Configuration

  val accessKey: String = configuration.getOptional[String]("aws.accessKey").getOrElse("changeme")
  val secretKey: String = configuration.getOptional[String]("aws.secretKey").getOrElse("changeme")
  val credentials = new BasicAWSCredentials(accessKey, secretKey)
  val credentialsProvider = new AWSStaticCredentialsProvider(credentials)
  val region: Regions = Regions.fromName(configuration.getOptional[String]("aws.region").getOrElse("us-east-1"))
}
