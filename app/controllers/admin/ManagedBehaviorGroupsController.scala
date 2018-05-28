package controllers.admin


import com.google.inject.Provider
import com.mohiva.play.silhouette.api.Silhouette
import controllers.RemoteAssets
import javax.inject.Inject
import json.BehaviorGroupData
import models.accounts.user.User
import models.behaviors.behaviorgroup.BehaviorGroup
import models.silhouette.EllipsisEnv
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
import services.DataService
import services.caching.CacheService
import utils.FutureSequencer

import scala.concurrent.{ExecutionContext, Future}


class ManagedBehaviorGroupsController @Inject() (
                                                  val silhouette: Silhouette[EllipsisEnv],
                                                  val dataService: DataService,
                                                  val cacheService: CacheService,
                                                  val configuration: Configuration,
                                                  val assetsProvider: Provider[RemoteAssets],
                                                  implicit val ec: ExecutionContext
                                                ) extends AdminAuth {

  def list(teamId: String) = silhouette.SecuredAction.async { implicit request =>
    withIsAdminCheck(() => {
      val user = request.identity
      for {
        maybeTeam <- dataService.teams.find(teamId)
        result <- maybeTeam.map { team =>
          for {
            users <- dataService.users.allFor(team)
            usersData <- FutureSequencer.sequence(users, (ea: User) =>
              dataService.users.userDataFor(ea, team)
            )
            managedGroups <- dataService.managedBehaviorGroups.allFor(team)
            allGroups <- dataService.behaviorGroups.allFor(team)
            groupData <- FutureSequencer.sequence(allGroups, (ea: BehaviorGroup) =>
              BehaviorGroupData.maybeFor(ea.id, user, None, dataService, cacheService)
            ).map(_.flatten)
          } yield {
            val withGroupData = managedGroups.flatMap { ea =>
              groupData.find(_.id.contains(ea.groupId)).map { groupData =>
                (ea, groupData)
              }
            }.sortBy { case(_, groupData) => groupData.name }
            val managedGroupIds = managedGroups.map(_.groupId)
            val otherNamedGroupData =
              groupData.
                filterNot(d => d.id.exists(managedGroupIds.contains)).
                filter(_.name.isDefined).
                sortBy(_.name)
            Ok(views.html.admin.managedBehaviorGroups.list(viewConfig(None), team, withGroupData, otherNamedGroupData, usersData.sortBy(_.userNameOrDefault.toLowerCase)))
          }
        }.getOrElse(Future.successful(NotFound(s"Team not found: $teamId")))

      } yield result
    })
  }

  case class AddManagedFormInfo(groupId: String)

  private val addManagedForm = Form(
    mapping(
      "groupId" -> nonEmptyText
    )(AddManagedFormInfo.apply)(AddManagedFormInfo.unapply)
  )

  def add(teamId: String) = silhouette.SecuredAction.async { implicit request =>
    withIsAdminCheck(() => {
      addManagedForm.bindFromRequest.fold(
        formWithErrors => {
          Future.successful(BadRequest(formWithErrors.errorsAsJson))
        },
        info => {
          for {
            maybeGroup <- dataService.behaviorGroups.findWithoutAccessCheck(info.groupId)
            result <- maybeGroup.map { group =>
              dataService.managedBehaviorGroups.ensureFor(group, None).map { _ =>
                Redirect(routes.ManagedBehaviorGroupsController.list(teamId))
              }
            }.getOrElse(Future.successful(NotFound(s"Skill not found: ${info.groupId}")))
          } yield result
        }
      )
    })
  }

  def delete(groupId: String) = silhouette.SecuredAction.async { implicit request =>
    withIsAdminCheck(() => {
      for {
        maybeGroup <- dataService.behaviorGroups.findWithoutAccessCheck(groupId)
        result <- maybeGroup.map { group =>
          dataService.managedBehaviorGroups.deleteFor(group).map { _ =>
            Redirect(routes.ManagedBehaviorGroupsController.list(group.team.id))
          }
        }.getOrElse(Future.successful(NotFound(s"Skill not found: ${groupId}")))
      } yield result
    })
  }

  case class SetContactInfo(contactId: Option[String])

  private val setContactForm = Form(
    mapping(
      "contactId" -> optional(nonEmptyText)
    )(SetContactInfo.apply)(SetContactInfo.unapply)
  )

  def setContact(groupId: String) = silhouette.SecuredAction.async { implicit request =>
    withIsAdminCheck(() => {
      setContactForm.bindFromRequest.fold(
        formWithErrors => {
          Future.successful(BadRequest(formWithErrors.errorsAsJson))
        },
        info => {
          for {
            maybeGroup <- dataService.behaviorGroups.findWithoutAccessCheck(groupId)
            maybeContact <- info.contactId.map { contactId =>
              dataService.users.find(contactId)
            }.getOrElse(Future.successful(None))
            result <- maybeGroup.map { group =>
              dataService.managedBehaviorGroups.ensureFor(group, maybeContact).map { _ =>
                Redirect(routes.ManagedBehaviorGroupsController.list(group.team.id))
              }
            }.getOrElse(Future.successful(NotFound(s"Skill not found: ${groupId}")))
          } yield result
        }
      )
    })
  }

}


