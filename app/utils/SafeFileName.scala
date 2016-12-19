package utils

object SafeFileName {

  def forName(name: String): String = {
    name.trim.replaceAll("""\s""", "_").replaceAll("""[^A-Za-z0-9_-]""", "")
  }

}
