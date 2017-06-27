package controllers

import models.IDs
import models.accounts.user.User
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.defaultstorageitem.DefaultStorageItemService
import models.behaviors.events.Event
import models.team.Team
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsArray, JsObject}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import sangria.schema.Schema
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
                     dataService: DataService,
                     graphQLService: GraphQLService
                   ) = {
    val token = IDs.next
    val mockGroup = mock[BehaviorGroup]
    when(dataService.behaviorGroups.findForInvocationToken(token)).thenReturn(Future.successful(Some(mockGroup)))
    when(graphQLService.runQuery(mockGroup, query, None, None)).thenReturn(Future.successful(Some(JsObject(Map("data" -> JsArray())))))
    token
  }

  "query" should {

    "respond with a valid result" in new ControllerTestContext {
      running(app) {
        val query =
          """{
            |  foo {
            |    bar
            |  }
            |}
          """.stripMargin
        val token = setUpMocksFor(team, user, query, dataService, graphQLService)
        val request = FakeRequest(controllers.routes.GraphQLController.query(token, query))
        val result = route(app, request).get
        status(result) mustBe OK
        val resultStr = contentAsString(result)
        println(resultStr)
      }
    }

  }


}
