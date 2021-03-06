package controllers

import models.IDs
import models.accounts.user.User
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.events.Event
import models.team.Team
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsArray, JsObject, JsString, JsValue}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{DataService, GraphQLService}
import support.ControllerTestContext

import scala.concurrent.Future

class GraphQLControllerSpec extends PlaySpec with MockitoSugar {

  case class APITestData(
                          token: String,
                          event: Event
                        )

  def setUpMocksFor(
                     team: Team,
                     user: User,
                     query: String,
                     queryResult: JsValue,
                     dataService: DataService,
                     graphQLService: GraphQLService
                   ) = {
    val token = IDs.next
    val mockGroup = mock[BehaviorGroup]
    when(dataService.behaviorGroups.findForInvocationToken(token)).thenReturn(Future.successful(Some(mockGroup)))
    when(dataService.users.findForInvocationToken(token)).thenReturn(Future.successful(Some(user)))
    when(graphQLService.runQuery(mockGroup, user, query, None, None)).thenReturn(Future.successful(queryResult))
    token
  }

  val madeUpQuery = """{
                      |  foo {
                      |    bar
                      |  }
                      |}
                    """.stripMargin

  def bodyFor(
              token: String,
              query: String,
              maybeOperationName: Option[String] = None,
              maybeVariables: Option[String] = None
            ): JsValue = {
    val parts = Seq(
      Some(("token", JsString(token))),
      Some(("query", JsString(query))),
      maybeOperationName.map { n => ("operationName", JsString(n)) },
      maybeVariables.map { v => ("variables", JsString(v)) }
    ).flatten
    JsObject(parts)
  }

  "query" should {

    "respond with a valid result" in new ControllerTestContext {
      running(app) {
        val queryResult = JsObject(Map("data" -> JsArray(Seq(JsString("foo")))))
        val token = setUpMocksFor(team, user, madeUpQuery, queryResult, dataService, graphQLService)
        val request = FakeRequest(controllers.routes.GraphQLController.query()).withJsonBody(bodyFor(token, madeUpQuery))
        val result = route(app, request).get
        status(result) mustBe OK
        contentAsJson(result) mustBe queryResult
      }
    }

    "respond with a 200 + errors if present" in new ControllerTestContext {
      running(app) {
        val queryResult = JsObject(Map("errors" -> JsArray(Seq(JsString("foo")))))
        val token = setUpMocksFor(team, user, madeUpQuery, queryResult, dataService, graphQLService)
        val request = FakeRequest(controllers.routes.GraphQLController.query()).withJsonBody(bodyFor(token, madeUpQuery))
        val result = route(app, request).get
        status(result) mustBe OK
        contentAsJson(result) mustBe queryResult
      }
    }

  }


}
