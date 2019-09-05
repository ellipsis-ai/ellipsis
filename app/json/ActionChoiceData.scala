package json

import models.behaviors.ActionChoice

case class ActionChoiceData(
                             label: String,
                             actionName: String,
                             args: Option[Seq[ActionArgData]],
                             allowOthers: Option[Boolean],
                             allowMultipleSelections: Option[Boolean],
                             userId: String,
                             originatingBehaviorVersionId: String,
                             quiet: Option[Boolean],
                             skillId: Option[String],
                             useDialog: Option[Boolean]
                           )

object ActionChoiceData {
  def from(choice: ActionChoice) = ActionChoiceData(
    label = choice.label,
    actionName = choice.actionName,
    args = choice.args.map(_.map(ea => ActionArgData(ea.name, ea.value))),
    allowOthers = choice.allowOthers,
    allowMultipleSelections = choice.allowMultipleSelections,
    userId = choice.userId,
    originatingBehaviorVersionId = choice.originatingBehaviorVersionId,
    quiet = choice.quiet,
    skillId = choice.skillId,
    useDialog = choice.useDialog
  )
}

case class ActionArgData(name: String, value: String)
