package models.behaviors.form

sealed trait FormElement {

}

case class TextElement(
                        content: String
                      ) extends FormElement

case class BehaviorInputsElement(
                                  behaviorId: String
                                ) extends FormElement
