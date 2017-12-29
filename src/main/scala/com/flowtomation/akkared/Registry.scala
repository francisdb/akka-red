package com.flowtomation.akkared

import com.flowtomation.akkared.nodes.core._
import scala.collection.immutable.Seq

class Registry {

  // TODO add modules / nodesets


  def getNodeList: Seq[NodeType] = {
    // TODO we probably want this discovered somehow
    Seq(Debug, Inject)
  }

  def getNode(nodeType: String): Option[NodeType] = {
    getNodeList.find(_.name == nodeType)
  }
}
