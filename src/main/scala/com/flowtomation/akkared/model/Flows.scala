package com.flowtomation.akkared.model

import play.api.libs.json.JsValue
import collection.immutable.Seq

case class ItemId(id: String) extends AnyVal

case class Flow(
  tab: Tab,
  nodes: Seq[Node]
)

case class Tab(
  id: ItemId,
  label: String,
  disabled: Boolean,
  info: String
)

case class Node(
  id: ItemId,
  name: String,
  `type`: String,
  x: Double,
  y: Double,
  wires: Seq[Seq[ItemId]], // multiple outputs that can go to multiple nodes (single input)
  otherProperties: Map[String, JsValue]
)