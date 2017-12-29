package com.flowtomation.akkared.format

import com.flowtomation.akkared.model.{Flows, ItemId, FlowNode, FlowTab}
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

import scala.reflect.runtime.universe._
import scala.collection.immutable.Seq

object FlowsFormat{

  private case class NodeItemBase(
    id: ItemId,
    name: String,
    `type`: String,
    x: Double,
    y: Double,
    z: ItemId, // tabId
    wires: Seq[Seq[ItemId]]
  )
  private final val keys = classAccessors[NodeItemBase]
  //println(keys)
  private sealed trait JsonFlowItem
  private case class JsonTabItem(
    id: ItemId,
    label: String,
    disabled: Boolean,
    info: String
  ) extends JsonFlowItem


  private case class JsonNodeItem(
    base: NodeItemBase,
    rest: Map[String, JsValue]
  ) extends JsonFlowItem

  private implicit val itemIdFormat = Format[ItemId](
    j => j.validate[String].map(ItemId),
    i => JsString(i.id)
  )

  private implicit val flowItemFormat: Format[JsonFlowItem] = new Format[JsonFlowItem] {

    private implicit val tabItemFormat = Json.format[JsonTabItem]
    private val tabType = Json.obj("type" -> "tab")
    private implicit val nodeItemBaseFormat = Json.format[NodeItemBase]

    def reads(json: JsValue): JsResult[JsonFlowItem] = {
      json.validate[JsObject].flatMap{ obj =>
        (obj \ "type").validate[String].flatMap{
          case "tab" =>
            obj.validate[JsonTabItem]
          case _ =>
            obj.validate[NodeItemBase].map{ base =>
              val rest = keys.foldLeft(obj){case (acc, v) => acc - v}
              JsonNodeItem(base, rest.as[Map[String, JsValue]])
            }
        }
      }
    }

    override def writes(o: JsonFlowItem): JsValue = {
      o match {
        case t: JsonTabItem => tabItemFormat.writes(t) ++ tabType
        case n: JsonNodeItem => JsObject(n.rest) ++ nodeItemBaseFormat.writes(n.base)
      }
    }
  }


  implicit object FlowsFormat extends Format[Flows]{

    override def reads(json: JsValue): JsResult[Flows] = {
      json.validate[Seq[JsonFlowItem]].map{ jsonItems =>
        val items = jsonItems.map{
          case JsonTabItem(id, label, disabled, info) =>
            FlowTab(id, label, disabled, info)
          case JsonNodeItem(base, rest) =>
            FlowNode(base.id, base.z, base.name, base.`type`, base.x, base.y, base.wires, rest)
        }
        Flows(
          items
        )
      }
    }

    override def writes(o: Flows): JsValue = {
      val items = o.items.map{
        case FlowTab(id, label, disabled, info) =>
          JsonTabItem(id, label, disabled, info)
        case FlowNode(id, tabId, name, nodeType, x, y, wires, otherProperties) =>
          JsonNodeItem(NodeItemBase(id, name, nodeType, x, y, tabId, wires), otherProperties)
      }
      Json.toJson(items)
    }


  }

  private val versionedFlowsReads: Reads[(Flows, String)] = (
    (JsPath \ "flows").read[Flows] and
      (JsPath \ "rev").read[String]
    ).tupled

  private val versionedFlowsWrites: Writes[(Flows, String)] = Writes{ case (flows, revision) =>
    Json.obj(
      "flows" -> flows,
      "rev" -> revision
    )
  }

  implicit val versionedFlowsFormat: Format[(Flows, String)] = Format(versionedFlowsReads, versionedFlowsWrites)


  private def classAccessors[T: TypeTag]: List[String] = typeOf[T].members.collect {
    case m: MethodSymbol if m.isCaseAccessor => m.name.toString
  }.toList

}

