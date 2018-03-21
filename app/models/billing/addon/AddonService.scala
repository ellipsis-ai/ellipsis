package models.billing.addon

import com.chargebee.models.{Addon, Plan}
import models.billing.ChargebeeService

import scala.concurrent.Future


trait AddonService extends ChargebeeService {

  def create(data: AddonData, doNotLogError: Boolean=false): Future[Option[Addon]]

  def createStandardAddons(doNotLogError: Boolean=false): Future[Seq[Option[Addon]]]

  def allAddons(count: Int = 100): Future[Seq[Addon]]

  def delete(addon: Addon): Future[Option[Addon]]

  def delete(addons: Seq[Addon]): Future[Seq[Option[Addon]]]

}
