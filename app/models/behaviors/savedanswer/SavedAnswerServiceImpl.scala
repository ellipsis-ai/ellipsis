package models.behaviors.savedanswer

import java.time.OffsetDateTime
import javax.inject.Inject

import com.google.inject.Provider
import models.IDs
import models.accounts.user.User
import models.behaviors.behaviorparameter.BehaviorParameter
import models.behaviors.input.Input
import services.DataService
import drivers.SlickPostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class RawSavedAnswer(
                          id: String,
                          inputId: String,
                          valueString: String,
                          maybeUserId: Option[String],
                          createdAt: OffsetDateTime
                         )

class SavedAnswersTable(tag: Tag) extends Table[RawSavedAnswer](tag, "saved_answers") {

  def id = column[String]("id")
  def inputId = column[String]("input_id")
  def valueString = column[String]("value_string")
  def maybeUserId = column[Option[String]]("user_id")
  def createdAt = column[OffsetDateTime]("created_at")

  def * = (id, inputId, valueString, maybeUserId, createdAt) <> ((RawSavedAnswer.apply _).tupled, RawSavedAnswer.unapply _)
}

class SavedAnswerServiceImpl @Inject() (
                                         dataServiceProvider: Provider[DataService]
                                       ) extends SavedAnswerService {

  def dataService = dataServiceProvider.get

  import SavedAnswerQueries._

  private def maybeUserIdFor(input: Input, user: User): Option[String] = {
    if (input.isSavedForUser) { Some(user.id) } else { None }
  }

  def find(input: Input, user: User): Future[Option[SavedAnswer]] = {
    val action = findQueryFor(input.id, maybeUserIdFor(input, user)).result.map { r =>
      r.headOption.map(tuple2SavedAnswer)
    }
    dataService.run(action)
  }

  def ensureFor(input: Input, valueString: String, user: User): Future[SavedAnswer] = {
    val maybeUserId = maybeUserIdFor(input, user)
    val query = rawFindQueryFor(input.id, maybeUserId)
    val action = for {
      maybeExisting <- query.result.map { r =>
        r.headOption
      }
      raw <- maybeExisting.map { existing =>
        val updated = existing.copy(valueString = valueString)
        query.update(updated).map(_ => updated)
      }.getOrElse {
        val raw = RawSavedAnswer(IDs.next, input.id, valueString, maybeUserId, OffsetDateTime.now)
        (all += raw).map(_ => raw)
      }
    } yield SavedAnswer(raw.id, input, valueString, maybeUserId, raw.createdAt)
    dataService.run(action)
  }

  def maybeFor(user: User, param: BehaviorParameter): Future[Option[SavedAnswer]] = {
    if (param.input.isSaved) {
      val action = maybeForQuery(maybeUserIdFor(param.input, user), param.input.id).result.map { r =>
        r.headOption.map(tuple2SavedAnswer)
      }
      dataService.run(action)
    } else {
      Future.successful(None)
    }
  }

  def allFor(user: User, params: Seq[BehaviorParameter]): Future[Seq[SavedAnswer]] = {
    Future.sequence(params.map { param =>
      maybeFor(user, param)
    }).map(_.flatten)
  }

  def allFor(input: Input): Future[Seq[SavedAnswer]] = {
    val action = allForInputQuery(input.id).result.map { r =>
      r.map(tuple2SavedAnswer)
    }
    dataService.run(action)
  }

  def updateForInputId(maybeOldInputId: Option[String], newInputId: String): Future[Unit] = {
    maybeOldInputId.map { oldInputId =>
      val action = all.filter(_.inputId === oldInputId).map(_.inputId).update(newInputId).map(_ => {})
      dataService.run(action)
    }.getOrElse(Future.successful({}))
  }

  def deleteForUser(input: Input, user: User): Future[Int] = {
    val action = uncompiledRawFindQueryFor(input.id, Some(user.id)).delete
    dataService.run(action)
  }

  def deleteAllFor(input: Input): Future[Int] = {
    val action = rawFindQueryIgnoringUserFor(input.id).delete
    dataService.run(action)
  }

}
