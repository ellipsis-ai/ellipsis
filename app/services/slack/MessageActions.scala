package services.slack

trait MessageActions {

  type T <: MessageAction

  val actions: Seq[T]

}
