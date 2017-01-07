package drivers

import com.github.tminglei.slickpg._

trait SlickPostgresDriver extends ExPostgresDriver
  with PgDate2Support
  with PgJsonSupport
  with PgPlayJsonSupport {
  def pgjson = "jsonb"

  override val api = ExtendedAPI

  object ExtendedAPI extends API
    with DateTimeImplicits
    with JsonImplicits
}

object SlickPostgresDriver extends SlickPostgresDriver
