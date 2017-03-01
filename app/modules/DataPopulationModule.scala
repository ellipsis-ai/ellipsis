package modules

import com.google.inject.AbstractModule
import data._
import net.codingwell.scalaguice.ScalaModule

class DataPopulationModule extends AbstractModule with ScalaModule {

  override def configure() = {
    bind(classOf[OAuth2ApiPopulator]).asEagerSingleton()
    bind(classOf[SimpleTokenApiPopulator]).asEagerSingleton()
    bind(classOf[EnsureGroups]).asEagerSingleton()
    bind(classOf[EnsureInvocationLogEntryUsers]).asEagerSingleton()
  }

}
