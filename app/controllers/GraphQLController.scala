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
                        query: String,
                        maybeOperationName: Option[String],
                        maybeVariables: Option[String]
                      )

  private val queryForm = Form(
    mapping(
      "query" -> nonEmptyText,
      "operationName" -> optional(nonEmptyText),
      "variables" -> optional(nonEmptyText)
    )(QueryInfo.apply)(QueryInfo.unapply)
  )

  def query(token: String) = Action.async { implicit request =>
    queryForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(formWithErrors.toString))
      },
      info => {
        for {
          maybeBehaviorGroup <- dataService.behaviorGroups.findForInvocationToken(token)
          maybeResult <- maybeBehaviorGroup.map { group =>
            graphQL.runQuery(group, info.query, info.maybeOperationName, info.maybeVariables)
          }.getOrElse(Future.successful(None))
        } yield {
          maybeResult.map { result =>
            Ok(result.toString)
          }.getOrElse(NotFound(""))
        }
      }
    )
  }

}
