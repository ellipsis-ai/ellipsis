package json

trait DialogParams

case class SlackDialogParams(
                              title: String,
                              callback_id: String,
                              elements: Seq[SlackDialogInput],
                              submit_label: String,
                              notify_on_cancel: Boolean,
                              state: String
                            ) extends DialogParams
