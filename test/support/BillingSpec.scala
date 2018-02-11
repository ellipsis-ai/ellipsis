package support

import java.sql.Timestamp
import java.time.Instant

import java.util.Calendar
import scala.concurrent.duration._

import com.chargebee.models.TimeMachine
import models.billing.ChargebeeService

import scala.concurrent.{Await, Future, blocking}


trait BillingSpec extends DBSpec with ChargebeeService  {

  def runNowAndBePatient[T](future: Future[T]): T = {
    Await.result(future, 120.seconds)
  }

  def startAfresh: Future[TimeMachine] = {
    Future {
      blocking {
        TimeMachine.startAfresh("delorean")
          .genesisTime(Timestamp.from(Instant.now.minusSeconds(3600*24*90)))
          .request(chargebeeEnv)
          .timeMachine()
          .waitForTimeTravelCompletion(chargebeeEnv)
      }
    }
  }

//  Timestamp old;
//  ZonedDateTime zonedDateTime = old.toInstant().atZone(ZoneId.of("UTC"));
//  Timestamp new = Timestamp.from(zonedDateTime.plus(14, ChronoUnit.DAYS).toInstant());

  private def addDays(days: Int, timestamp: Timestamp): Timestamp = {
    val cal = Calendar.getInstance()
    cal.setTimeInMillis(timestamp.getTime())
    cal.add(Calendar.DAY_OF_MONTH, days)
    new Timestamp(cal.getTime().getTime())
  }

  def moveForward(timeMachine: TimeMachine, forDays: Int) = {
    Future{
      blocking {
        TimeMachine.travelForward("delorean")
          .destinationTime(addDays(forDays, timeMachine.destinationTime()))
          .request(chargebeeEnv)
          .timeMachine()
          .waitForTimeTravelCompletion(chargebeeEnv)
      }
    }
  }

  def clearCustomerData = {
    for {
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

  def restChargebeeSite: Future[TimeMachine] = {
    for {
      timeMachine <- startAfresh

      // Chargebee needs a moment to delete the Subscriptions
      _ <- Future { Thread.sleep(2000) }

      _ <- clearPlansAndAddons
      _ <- dataService.plans.createStandardPlans()
      _ <- dataService.addons.createStandardAddons()
    } yield {
      timeMachine
    }
  }

}
