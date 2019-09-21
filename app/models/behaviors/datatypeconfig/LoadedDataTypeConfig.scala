package models.behaviors.datatypeconfig

import models.behaviors.behaviorversion.BehaviorVersion
import services.DataService
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext

case class LoadedDataTypeConfig(
                                 config: DataTypeConfig,
                                 behaviorVersion: BehaviorVersion
                               ) {

  def usesCode: Boolean = config.usesCode

}

object LoadedDataTypeConfig {
  def fromAction(config: DataTypeConfig, dataService: DataService)(implicit ec: ExecutionContext): DBIO[LoadedDataTypeConfig] = {
    dataService.behaviorVersions.findWithoutAccessCheckAction(config.behaviorVersion.id).map { maybeBehaviorVersion =>
      LoadedDataTypeConfig(config, maybeBehaviorVersion.get)
    }
  }
}
