package json

import models.behaviors.behaviorparameter.{BuiltInTextualType, YesNoType}
import models.behaviors.input.Input

trait DialogInput

case class SlackDialogSelectOption(label: String, value: String)

sealed trait SlackDialogInput {
  val label: String
  val name: String
  val `type`: String
  val subtype: Option[String] = None
  val value: Option[String] = None
  val placeholder: Option[String] = None
  val options: Option[Seq[SlackDialogSelectOption]] = None
}

case class SlackDialogTextInput(
                                 label: String,
                                 name: String,
                                 override val placeholder: Option[String],
                                 `type`: String = "text"
                               ) extends SlackDialogInput

case class SlackDialogSelectInput(
                                   label: String,
                                   name: String,
                                   override val options: Option[Seq[SlackDialogSelectOption]],
                                   `type`: String = "select"
                                 ) extends SlackDialogInput

object SlackDialogInput {
  def maybeFromInput(input: Input): Option[SlackDialogInput] = {
    input.paramType match {
      case _: BuiltInTextualType => Some(SlackDialogTextInput(input.question, input.name, None))
      case YesNoType => Some(SlackDialogSelectInput(input.question, input.name, Some(Seq(
        SlackDialogSelectOption("Yes", "yes"),
        SlackDialogSelectOption("No", "no")
      ))))
      case _ => None
    }
  }
}
