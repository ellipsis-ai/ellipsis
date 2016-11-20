package models.behaviors.savedanswer

import javax.inject.Inject

import com.github.tototoshi.slick.PostgresJodaSupport._
import com.google.inject.Provider
import models.IDs
import models.accounts.user.User
import models.behaviors.behaviorparameter.BehaviorParameter
import models.behaviors.input.Input
import org.joda.time.DateTime
import services.DataService
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class RawSavedAnswer(
                          id: String,
                          inputId: String,
                          valueString: String,
                          maybeUserId: Option[String],
                          createdAt: DateTime
                         )

class SavedAnswersTable(tag: Tag) extends Table[RawSavedAnswer](tag, "saved_answers") {

  def id = column[String]("id")
  def inputId = column[String]("input_id")
  def valueString = column[String]("value_string")
  def maybeUserId = column[Option[String]]("user_id")
  def createdAt = column[DateTime]("created_at")

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
        val raw = RawSavedAnswer(IDs.next, input.id, valueString, maybeUserId, DateTime.now)
        (all += raw).map(_ => raw)
      }
    } yield SavedAnswer(raw.id, input, valueString, maybeUserId, raw.createdAt)
    dataService.run(action)
  }

  def maybeFor(user: User, param: BehaviorParameter): Future[Option[SavedAnswer]] = {
    val action = maybeForQuery(maybeUserIdFor(param.input, user), param.input.id).result.map { r =>
      r.headOption.map(tuple2SavedAnswer)
    }
    dataService.run(action)
  }

  def allFor(user: User, params: Seq[BehaviorParameter]): Future[Seq[SavedAnswer]] = {
    Future.sequence(params.map { param =>
      maybeFor(user, param)
    }).map(_.flatten)
  }

  def updateForInputId(maybeOldInputId: Option[String], newInputId: String): Future[Unit] = {
    maybeOldInputId.map { oldInputId =>
      val action = all.filter(_.inputId === oldInputId).map(_.inputId).update(newInputId).map(_ => {})
      dataService.run(action)
    }.getOrElse(Future.successful({}))
  }

}
