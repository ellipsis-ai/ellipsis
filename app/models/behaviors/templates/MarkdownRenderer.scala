package models.behaviors.templates

import models.behaviors.templates.ast._
import play.api.libs.json._


case class Environment(maybeParent: Option[Environment], data: Map[String, JsValue], result: JsLookupResult) {

  def lookUp(substitution: Substitution): JsLookupResult = {
    if (substitution.hasSuccessResult) {
      substitution.getFrom(result)
    } else {
      data.get(substitution.nameToLookUpInEnvironment).map { v =>
        substitution.pathSegmentsToLookUpInEnvironment.foldLeft(JsDefined.apply(v):JsLookupResult)((a,b) => a \ b)
      }.getOrElse {
        maybeParent.map { parent =>
          parent.lookUp(substitution)
        }.getOrElse {
          JsUndefined(s"${substitution.pathString} not found")
        }
      }
    }
  }

  def newWith(newData: Map[String, JsValue]): Environment = {
    Environment(Some(this), newData, result)
  }
}

case class MarkdownRenderer(
                             stringBuilder: StringBuilder,
                             result: JsLookupResult,
                             inputs: Seq[(String, JsValue)] = Seq()
                            ) {

  var environment = Environment(None, inputs.toMap, result)

  def push(data: Map[String, JsValue]): Unit = {
    environment = environment.newWith(data)
  }

  def pop(): Unit = {
    environment.maybeParent.foreach { parent =>
      environment = parent
    }
  }

  def lookUp(substitution: Substitution): JsLookupResult = environment.lookUp(substitution)

  def notFound(name: String): String = {
    s"**$name not found**"
  }

  private def printJsValue(value: JsValue): String = {
    value match {
      case s: JsString => s.value
      case _ => value.toString
    }
  }

  def visit(block: Block): Unit = {
    block.children.foreach { ea =>
      ea.accept(this)
    }
  }

  def visit(text: Text): Unit = {
    stringBuilder.append(text.value)
  }

  def visit(substitution: Substitution): Unit = {
    val valueString = printJsValue(lookUp(substitution).getOrElse(JsString(notFound(substitution.pathString))))
    stringBuilder.append(valueString)
  }

  def visit(iteration: Iteration): Unit = {
    lookUp(Substitution(iteration.list)) match {
      case lookupResult: JsUndefined => stringBuilder.append(notFound(iteration.list.dotString))
      case lookupResult: JsDefined => {
        lookupResult.value match {
          case arr: JsArray => {
            arr.as[Seq[JsValue]].foreach { ea =>
              push(Map(iteration.item.name -> ea))
              visit(iteration.body)
              pop()
            }
          }
          case _ => stringBuilder.append(s"**${iteration.list.dotString} is not a list**")
        }
      }
    }
  }

  def visit(conditional: Conditional): Unit = {
    val dotString = conditional.condition.dotString
    lookUp(Substitution(conditional.condition)) match {
      case lookupResult: JsUndefined => stringBuilder.append(notFound(dotString))
      case lookupResult: JsDefined => {
        lookupResult.value match {
          case bool: JsBoolean => {
            if (bool.value) {
              visit(conditional.body)
            } else {
              conditional.maybeElseBody.foreach(visit)
            }
          }
          case nonBoolean => stringBuilder.append(s"**$dotString is not a boolean** (${nonBoolean.toString})")
        }
      }
    }
  }
}
