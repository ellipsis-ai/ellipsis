package json.slack.dialogs

import models.behaviors.behaviorparameter._
import models.behaviors.input.Input

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
                                 override val subtype: Option[String],
                                 `type`: String = "text"
                               ) extends SlackDialogInput

case class SlackDialogSelectInput(
                                   label: String,
                                   name: String,
                                   override val options: Option[Seq[SlackDialogSelectOption]],
                                   `type`: String = "select"
                                 ) extends SlackDialogInput

case class SlackDialogSelectOption(label: String, value: String)

object SlackDialogInput {
  def subtypeFor(textType: BuiltInTextualType): Option[String] = {
    textType match {
      case NumberType => Some("number")
      case _ => None
    }
  }

  def placeholderFor(textType: BuiltInTextualType): Option[String] = {
    textType match {
      case NumberType => Some("Enter a number")
      case DateTimeType => Some("Enter a date/time")
      case _ => None
    }
  }

  def maybeFromInput(input: Input): Option[SlackDialogInput] = {
    input.paramType match {
      case textType: BuiltInTextualType => {
        Some(SlackDialogTextInput(input.question, input.name, placeholderFor(textType), subtypeFor(textType)))
      }
      case YesNoType => Some(SlackDialogSelectInput(input.question, input.name, Some(Seq(
        SlackDialogSelectOption("Yes", "yes"),
        SlackDialogSelectOption("No", "no")
      ))))
      case _ => None
    }
  }
}
