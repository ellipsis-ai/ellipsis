package drivers

import com.github.tminglei.slickpg._

trait SlickPostgresDriver extends ExPostgresProfile
  with PgDate2Support
  with PgJsonSupport
  with PgPlayJsonSupport {

  override val pgjson = "jsonb"

  override val api = ExtendedAPI

  object ExtendedAPI extends API
    with DateTimeImplicits
    with JsonImplicits
}

object SlickPostgresDriver extends SlickPostgresDriver
