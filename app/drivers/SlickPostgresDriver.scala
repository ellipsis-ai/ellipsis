package drivers

import com.github.tminglei.slickpg._

trait SlickPostgresDriver extends ExPostgresDriver
  with PgDateSupportJoda
  with PgJsonSupport {
  def pgjson = "jsonb"

  override val api = ExtendedAPI

  object ExtendedAPI extends API
    with DateTimeImplicits
    with JsonImplicits
}

object SlickPostgresDriver extends SlickPostgresDriver
