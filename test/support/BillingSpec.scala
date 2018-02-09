package support

import com.chargebee.models.TimeMachine
import models.billing.ChargebeeService

import scala.concurrent.{Future, blocking}


trait BillingSpec extends DBSpec with ChargebeeService  {

  def startAfresh: Future[TimeMachine] = {
    Future {
      blocking {
        TimeMachine.startAfresh("delorean").request(chargebeeEnv)
          .timeMachine().waitForTimeTravelCompletion(chargebeeEnv)
      }
    }
  }

  def backToPresent: Future[TimeMachine] = {
    Future {
      blocking {
        TimeMachine.travelForward("delorean").request(chargebeeEnv)
          .timeMachine().waitForTimeTravelCompletion(chargebeeEnv)
      }
    }
  }

  def clearCustomerData = {
    for {
//      _ <- startAfresh
//      _ <- backToPresent
      subs <- dataService.subscriptions.allSubscriptions()
      _ <- dataService.subscriptions.delete(subs)
      custs <- dataService.customers.allCustomers()
      _ <- dataService.customers.delete(custs)
    } yield {}
  }

  def clearPlansAndAddons = {
    for {
      addons <- dataService.addons.allAddons()
      plans <- dataService.plans.allPlans()
      _ <- dataService.addons.delete(addons)
      _ <- dataService.plans.delete(plans)
    } yield {}
  }

  def restChargebeeSite = {
    for {
      _ <- clearCustomerData

      // Chargebee needs a moment to delete the Subscriptions
      _ <- Future { Thread.sleep(2000) }

      _ <- clearPlansAndAddons
      _ <- dataService.plans.createStandardPlans
      _ <- dataService.addons.createStandardAddons
    } yield {}
  }

}
