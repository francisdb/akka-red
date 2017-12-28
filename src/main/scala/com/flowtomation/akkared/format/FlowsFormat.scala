package com.flowtomation.akkared.format

import com.flowtomation.akkared.model.{Flow, ItemId, Node, Tab}
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
  private sealed trait FlowItem
  private case class TabItem(
    id: ItemId,
    label: String,
    disabled: Boolean,
    info: String
  ) extends FlowItem


  private case class NodeItem(
    base: NodeItemBase,
    rest: Map[String, JsValue]
  ) extends FlowItem

  private implicit val itemIdFormat = Format[ItemId](
    j => j.validate[String].map(ItemId),
    i => JsString(i.id)
  )

  private implicit val flowItemFormat: Format[FlowItem] = new Format[FlowItem] {

    private implicit val tabItemFormat = Json.format[TabItem]
    private val tabType = Json.obj("type" -> "tab")
    private implicit val nodeItemBaseFormat = Json.format[NodeItemBase]

    def reads(json: JsValue): JsResult[FlowItem] = {
      json.validate[JsObject].flatMap{ obj =>
        (obj \ "type").validate[String].flatMap{
          case "tab" =>
            obj.validate[TabItem]
          case _ =>
            obj.validate[NodeItemBase].map{ base =>
              val rest = keys.foldLeft(obj){case (acc, v) => acc - v}
              NodeItem(base, rest.as[Map[String, JsValue]])
            }
        }
      }
    }

    override def writes(o: FlowItem): JsValue = {
      o match {
        case t: TabItem => tabItemFormat.writes(t) ++ tabType
        case n: NodeItem => JsObject(n.rest) ++ nodeItemBaseFormat.writes(n.base)
      }
    }
  }


  implicit object FlowsFormat extends Format[Seq[Flow]]{

    override def reads(json: JsValue): JsResult[Seq[Flow]] = {
      json.validate[Seq[FlowItem]].map{ items =>
        val tabs = items.collect{case t:TabItem => t}
        val nodes = items.collect{case n:NodeItem => n}
        tabs.map{ tabItem =>
          Flow(
            Tab(tabItem.id, tabItem.label, tabItem.disabled, tabItem.info),
            nodes.filter(_.base.z == tabItem.id).map{ i =>
              Node(i.base.id, i.base.name, i.base.`type`, i.base.x, i.base.y, i.base.wires, i.rest)
            }
          )
        }
      }
    }

    override def writes(o: Seq[Flow]): JsValue = {
      val tabs = o.map(_.tab).map{ t =>
        TabItem(t.id, t.label, t.disabled, t.info)
      }
      val nodes = o.flatMap{ f =>
        f.nodes.map{ n =>
          NodeItem(NodeItemBase(n.id, n.name, n.`type`, n.x, n.y, f.tab.id, n.wires), n.otherProperties)
        }
      }
      val items: Seq[FlowItem] = tabs ++ nodes
      Json.toJson(items)
    }


  }

  implicit val versionedFlowsReads: Reads[(Seq[Flow], String)] = (
    (JsPath \ "flows").read[Seq[Flow]] and
      (JsPath \ "rev").read[String]
    ).tupled


  private def classAccessors[T: TypeTag]: List[String] = typeOf[T].members.collect {
    case m: MethodSymbol if m.isCaseAccessor => m.name.toString
  }.toList

}

