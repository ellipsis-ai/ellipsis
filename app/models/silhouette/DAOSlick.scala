package models.silhouette

import slick.driver.JdbcProfile
import play.api.db.slick.HasDatabaseConfigProvider

trait DAOSlick extends DBTableDefinitions with HasDatabaseConfigProvider[JdbcProfile]
