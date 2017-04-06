package modules

import com.google.inject.AbstractModule
import data._
import net.codingwell.scalaguice.ScalaModule
import utils.CitiesToTimeZones

class DataPopulationModule extends AbstractModule with ScalaModule {

  override def configure() = {
    bind(classOf[OAuth2ApiPopulator]).asEagerSingleton()
    bind(classOf[SimpleTokenApiPopulator]).asEagerSingleton()
    bind(classOf[EnsureExportIds]).asEagerSingleton()
    bind(classOf[CitiesToTimeZones]).asEagerSingleton()
  }

}
