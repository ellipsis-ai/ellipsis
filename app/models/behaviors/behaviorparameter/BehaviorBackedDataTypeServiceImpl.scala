package models.behaviors.behaviorparameter

import javax.inject.Inject

import com.google.inject.Provider
import models.IDs
import models.accounts.user.User
import models.behaviors.behavior.Behavior
import models.team.Team
import services.DataService
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class RawBehaviorBackedDataType(
                                      id: String,
                                      name: String,
                                      behaviorId: String
                                    )

class BehaviorBackedDataTypesTable(tag: Tag) extends Table[RawBehaviorBackedDataType](tag, "behavior_backed_data_types") {

  def id = column[String]("id", O.PrimaryKey)
  def name = column[String]("name")
  def behaviorId = column[String]("behavior_id")

  def * =
    (id, name, behaviorId) <> ((RawBehaviorBackedDataType.apply _).tupled, RawBehaviorBackedDataType.unapply _)
}

class BehaviorBackedDataTypeServiceImpl @Inject() (
                                               dataServiceProvider: Provider[DataService]
                                             ) extends BehaviorBackedDataTypeService {

  def dataService = dataServiceProvider.get

  import BehaviorBackedDataTypeQueries._

  def createFor(name: String, behavior: Behavior): Future[BehaviorBackedDataType] = {
    val raw = RawBehaviorBackedDataType(IDs.next, name, behavior.id)

    val action = (all += raw).map { _ =>
      BehaviorBackedDataType(raw.id, raw.name, behavior)
    }

    dataService.run(action)
  }

  def uncompiledAllForQuery(teamId: Rep[String]) = {
    joined.filter(_._2._1.teamId === teamId)
  }
  val allForQuery = Compiled(uncompiledAllForQuery _)

  def allFor(team: Team): Future[Seq[BehaviorBackedDataType]] = {
    val action = allForQuery(team.id).result.map { r =>
      r.map(tuple2DataType)
    }
    dataService.run(action)
  }

  def uncompiledForBehaviorQuery(behaviorId: Rep[String]) = {
    joined.filter(_._2._1.id === behaviorId)
  }
  val forBehaviorQuery = Compiled(uncompiledForBehaviorQuery _)

  def maybeFor(behavior: Behavior): Future[Option[BehaviorBackedDataType]] = {
    val action = forBehaviorQuery(behavior.id).result.map { r =>
      r.headOption.map(tuple2DataType)
    }
    dataService.run(action)
  }

  def updateName(id: String, name: String): Future[Unit] = {
    val action = all.filter(_.id === id).map(_.name).update(name).map { _ => {} }
    dataService.run(action)
  }

  def uncompiledFindQuery(id: Rep[String]) = {
    joined.filter(_._1.id === id)
  }
  val findQuery = Compiled(uncompiledFindQuery _)

  def findWithoutAccessCheck(id: String): Future[Option[BehaviorBackedDataType]] = {
    val action = findQuery(id).result.map { r =>
      r.headOption.map(tuple2DataType)
    }
    dataService.run(action)
  }

  def find(id: String, user: User): Future[Option[BehaviorBackedDataType]] = {
    findWithoutAccessCheck(id).map { maybeDataType =>
      maybeDataType.flatMap { dataType =>
        if (dataType.behavior.team.id == user.teamId) {
          Some(dataType)
        } else {
          None
        }
      }
    }
  }

  def delete(dataType: BehaviorBackedDataType): Future[Unit] = {
    for {
      _ <- dataService.run(all.filter(_.id === dataType.id).delete)
      _ <- dataService.behaviors.unlearn(dataType.behavior)
    } yield {}
  }

  def usesSearch(dataType: BehaviorBackedDataType): Future[Boolean] = {
    for {
      maybeCurrentVersion <- dataService.behaviors.maybeCurrentVersionFor(dataType.behavior)
      params <- maybeCurrentVersion.map { version =>
        dataService.behaviorParameters.allFor(version)
      }.getOrElse(Future.successful(Seq()))
    } yield {
      params.exists(_.name == BehaviorBackedDataTypeQueries.SEARCH_QUERY_PARAM)
    }
  }

}
