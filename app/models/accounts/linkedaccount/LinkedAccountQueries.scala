package models.accounts.linkedaccount

import drivers.SlickPostgresDriver.api._
import models.accounts.slack.SlackProvider
import models.accounts.user.{User, UserQueries}

object LinkedAccountQueries {

  val all = TableQuery[LinkedAccountsTable]
  val joined = all.join(UserQueries.all).on(_.userId === _.id)

  def uncompiledFindQuery(providerId: Rep[String], providerKey: Rep[String], teamId: Rep[String]) = {
    joined.
      filter { case(linked, user) => linked.providerId === providerId }.
      filter { case(linked, user) => linked.providerKey === providerKey }.
      filter { case(linked, user) => user.teamId === teamId }
  }
  val findQuery = Compiled(uncompiledFindQuery _)

  def tuple2LinkedAccount(tuple: (RawLinkedAccount, User)): LinkedAccount = {
    LinkedAccount(tuple._2, tuple._1.loginInfo, tuple._1.createdAt)
  }

  def uncompiledAllForQuery(userId: Rep[String]) = {
    joined.filter { case(_, u) => u.id === userId }
  }
  val allForQuery = Compiled(uncompiledAllForQuery _)

  def uncompiledForProviderForQuery(userId: Rep[String], providerId: Rep[String]) = {
    joined.
      filter { case(la, u) => la.providerId === providerId }.
      filter { case(la, u) => u.id === userId }
  }
  val forProviderForQuery = Compiled(uncompiledForProviderForQuery _)

  def uncompiledRawForProviderForQuery(userId: Rep[String], providerId: Rep[String]) = {
    all.filter(_.userId === userId).filter(_.providerId === providerId)
  }
  val rawForProviderForQuery = Compiled(uncompiledRawForProviderForQuery _)

  def uncompiledForSlackForQuery(userId: Rep[String]) = uncompiledForProviderForQuery(userId, SlackProvider.ID)
  val forSlackForQuery = Compiled(uncompiledForSlackForQuery _)

}
