package support

trait ControllerTestContextWithLoggedInUser extends ControllerTestContext {

  override lazy val identities = Seq(user.loginInfo -> user)

}
