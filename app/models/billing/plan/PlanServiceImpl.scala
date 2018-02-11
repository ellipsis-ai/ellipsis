package models.billing.plan


import javax.inject.Inject

import com.chargebee.models.Plan
import com.google.inject.Provider
import play.api.{Configuration, Logger}
import services.DataService

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future, blocking}


class PlanServiceImpl @Inject()(
                                 val configuration: Configuration,
                                 val dataServiceProvider: Provider[DataService],
                                 implicit val ec: ExecutionContext
                               ) extends PlanService {

  def dataService = dataServiceProvider.get

  def allPlans(count: Int = 100): Future[Seq[Plan]] = {
      Future {
        blocking {
          Plan.list().limit(count).request(chargebeeEnv)
        }
      }.map { result =>
        val buffer = ListBuffer[com.chargebee.models.Plan]()
        for (entry <- result) {
          buffer += entry.plan
        }
        buffer
      }
  }

  def find(id: String): Future[Option[Plan]] = {
    Future {
      blocking{
        Some(Plan.retrieve(id).request(chargebeeEnv).plan())
      }
    }.recover{
      case e: Throwable => {
        Logger.error(s"Error while finding Plan ${id}", e)
      }
      None
    }
  }

  def create(data: PlanData, doNotLogError: Boolean = false): Future[Option[Plan]] = {
    Future {
      blocking {
        Some(Plan.create()
          .id(data.id)
          .name(data.name)
          .invoiceName(data.invoiceName)
          .price(data.price)
          .request(chargebeeEnv)
          .plan())
      }
    }.recover {
      case e: Throwable => {
        if (!doNotLogError) Logger.error(s"Error while creating Plan with ${data}", e)
      }
      None
    }
  }

  def createStandardPlans(doNotLogError: Boolean = false): Future[Seq[Option[Plan]]] = {
      Future.sequence {
        StandardPlans.list.map(create(_, doNotLogError))
      }
  }

  def delete(plan: Plan): Future[Option[Plan]] = {
    Future {
      blocking {
        Some(Plan.delete(plan.id).request(chargebeeEnv).plan())
      }
    }.recover {
      case e: Throwable => {
        Logger.error(s"Error while deleting Plan with id ${plan.id}", e)
        None
      }
    }
  }

  def delete(plans: Seq[Plan]): Future[Seq[Option[Plan]]] = Future.sequence(plans.map(delete(_)))

}
