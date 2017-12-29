package com.flowtomation.akkared.model

import play.api.libs.json.JsValue
import collection.immutable.Seq

case class ItemId(id: String) extends AnyVal

sealed trait FlowItem{
  def id: ItemId
}

case class FlowTab(
  id: ItemId,
  label: String,
  disabled: Boolean,
  info: String
) extends FlowItem

case class FlowNode(
  id: ItemId,
  tabId: ItemId,
  name: String,
  `type`: String,
  x: Double,
  y: Double,
  wires: Seq[Seq[ItemId]], // multiple outputs that can go to multiple nodes (single input)
  otherProperties: Map[String, JsValue]
) extends FlowItem

object Flows{
  def apply(items: FlowItem*): Flows = Flows(items.to[Seq])
  val empty = Flows(Seq.empty)
}
case class Flows(
  items: Seq[FlowItem],
)
