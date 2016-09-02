package mocks

import javax.inject.Singleton

import models.accounts.logintoken.LoginTokenService
import models.accounts.user.UserService
import org.scalatest.mock.MockitoSugar
import slick.dbio.DBIO
import services.DataService

import scala.concurrent.Future

@Singleton
class MockDataService extends DataService with MockitoSugar {

  val users: UserService = mock[UserService]
  val loginTokens: LoginTokenService = mock[LoginTokenService]

  def run[T](action: DBIO[T]): Future[T] = throw new Exception("Don't call me")

}
