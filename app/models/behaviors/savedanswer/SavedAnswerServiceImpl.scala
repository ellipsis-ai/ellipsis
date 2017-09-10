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

import scala.concurrent.{ExecutionContext, Future}

class SavedAnswersTable(tag: Tag) extends Table[SavedAnswer](tag, "saved_answers") {

  def id = column[String]("id")
  def inputId = column[String]("input_id")
  def valueString = column[String]("value_string")
  def maybeUserId = column[Option[String]]("user_id")
  def createdAt = column[OffsetDateTime]("created_at")

  def * = (id, inputId, valueString, maybeUserId, createdAt) <> ((SavedAnswer.apply _).tupled, SavedAnswer.unapply _)
}

class SavedAnswerServiceImpl @Inject() (
                                         dataServiceProvider: Provider[DataService],
                                         implicit val ec: ExecutionContext
                                       ) extends SavedAnswerService {

  def dataService = dataServiceProvider.get

  import SavedAnswerQueries._

  private def maybeUserIdFor(input: Input, user: User): Option[String] = {
    if (input.isSavedForUser) { Some(user.id) } else { None }
  }

  def find(input: Input, user: User): Future[Option[SavedAnswer]] = {
    val action = findQueryFor(input.inputId, maybeUserIdFor(input, user)).result.map { r =>
      r.headOption
    }
    dataService.run(action)
  }

  def ensureFor(input: Input, valueString: String, user: User): Future[SavedAnswer] = {
    val maybeUserId = maybeUserIdFor(input, user)
    val query = rawFindQueryFor(input.inputId, maybeUserId)
    val action = for {
      maybeExisting <- query.result.map { r =>
        r.headOption
      }
      saved <- maybeExisting.map { existing =>
        val updated = existing.copy(valueString = valueString)
        query.update(updated).map(_ => updated)
      }.getOrElse {
        val answer = SavedAnswer(IDs.next, input.inputId, valueString, maybeUserId, OffsetDateTime.now)
        (all += answer).map(_ => answer)
      }
    } yield saved
    dataService.run(action)
  }

  def maybeForAction(user: User, param: BehaviorParameter): DBIO[Option[SavedAnswer]] = {
    if (param.input.isSaved) {
      maybeForQuery(maybeUserIdFor(param.input, user), param.input.inputId).result.map { r =>
        r.headOption
      }
    } else {
      DBIO.successful(None)
    }
  }

  def allForAction(user: User, params: Seq[BehaviorParameter]): DBIO[Seq[SavedAnswer]] = {
    DBIO.sequence(params.map { param =>
      maybeForAction(user, param)
    }).map(_.flatten)
  }

  def allFor(user: User, params: Seq[BehaviorParameter]): Future[Seq[SavedAnswer]] = {
    dataService.run(allForAction(user, params))
  }

  def allFor(inputId: String): Future[Seq[SavedAnswer]] = {
    val action = allForInputQuery(inputId).result
    dataService.run(action)
  }

  def updateForInputId(maybeOldInputId: Option[String], newInputId: String): Future[Unit] = {
    maybeOldInputId.map { oldInputId =>
      if (oldInputId != newInputId) {
        val action = all.filter(_.inputId === oldInputId).map(_.inputId).update(newInputId).map(_ => {})
        dataService.run(action)
      } else {
        Future.successful({})
      }
    }.getOrElse(Future.successful({}))
  }

  def deleteForUser(inputId: String, user: User): Future[Int] = {
    val action = uncompiledRawFindQueryFor(inputId, Some(user.id)).delete
    dataService.run(action)
  }

  def deleteAllFor(inputId: String): Future[Int] = {
    val action = rawFindQueryIgnoringUserFor(inputId).delete
    dataService.run(action)
  }

}
