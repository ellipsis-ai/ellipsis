package models.billing.addon

import javax.inject.Inject

import com.chargebee.models.Addon
import com.google.inject.Provider
import play.api.{Configuration, Logger}
import services.DataService

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future, blocking}


class AddonServiceImpl @Inject()(
                                 val configuration: Configuration,
                                 val dataServiceProvider: Provider[DataService],
                                 implicit val ec: ExecutionContext
                               ) extends AddonService {

  def dataService = dataServiceProvider.get

  def create(data: AddonData, doNotLogError: Boolean=false): Future[Option[Addon]] = {
    Future {
      blocking {
        Some(Addon.create()
          .id(data.id)
          .name(data.name)
          .invoiceName(data.invoiceName)
          .price(data.price)
          .unit(data.unit)
          .currencyCode(data.currencyCode)
          .chargeType(Addon.ChargeType.NON_RECURRING)
          .`type`(Addon.Type.QUANTITY)
          .request(chargebeeEnv)
          .addon())
      }
    }.recover {
      case e: Throwable => {
        if (!doNotLogError) Logger.error(s"Error while creating Addon ${data}", e)
      }
        None
    }
  }


  def createStandardAddons(doNotLogError: Boolean=false): Future[Seq[Option[Addon]]] = {
    Future.sequence {
      StandardAddons.list.map(data => create(data, doNotLogError))
    }
  }

  def allAddons(count: Int = 100): Future[Seq[Addon]] = {
    Future {
      blocking {
        Addon.list().limit(count).request(chargebeeEnv)
      }
    }.map { result =>
      val buffer = ListBuffer[Addon]()
      for (entry <- result) {
        buffer += entry.addon
      }
      buffer
    }
  }

  def delete(addon: Addon): Future[Option[Addon]] = {
    Future {
      blocking {
        Addon.delete(addon.id).request(chargebeeEnv).addon()
      }
    }.map(Some(_)).recover {
      case e: Throwable => {
        Logger.error(s"Error while deleting Addon with id ${addon.id}", e)
        None
      }
    }
  }

  def delete(addons: Seq[Addon]): Future[Seq[Option[Addon]]] = Future.sequence(addons.map(delete(_)))

}
