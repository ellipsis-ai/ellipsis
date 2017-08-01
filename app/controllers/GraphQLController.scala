package controllers

import javax.inject.Inject

import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.mvc.Action
import services.{DataService, GraphQLService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class GraphQLController @Inject() (
                                    val messagesApi: MessagesApi,
                                    val configuration: Configuration,
                                    val dataService: DataService,
                                    val graphQL: GraphQLService
                                  ) extends EllipsisController {

  case class QueryInfo(
                        token: String,
                        query: String,
                        maybeOperationName: Option[String],
                        maybeVariables: Option[String]
                      )

  private val queryForm = Form(
    mapping(
      "token" -> nonEmptyText,
      "query" -> nonEmptyText,
      "operationName" -> optional(nonEmptyText),
      "variables" -> optional(nonEmptyText)
    )(QueryInfo.apply)(QueryInfo.unapply)
  )

  def query = Action.async { implicit request =>
    queryForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(formWithErrors.toString))
      },
      info => {
        for {
          maybeUser <- dataService.users.findForInvocationToken(info.token)
          maybeBehaviorGroup <- dataService.behaviorGroups.findForInvocationToken(info.token)
          maybeResult <- (for {
            user <- maybeUser
            group <- maybeBehaviorGroup
          } yield {
            graphQL.runQuery(group, user, info.query, info.maybeOperationName, info.maybeVariables).map(Some(_))
          }).getOrElse(Future.successful(None))
        } yield {
          maybeResult.map { result =>
            Ok(result.toString)
          }.getOrElse(NotFound(""))
        }
      }
    )
  }

}
