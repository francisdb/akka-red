package com.flowtomation.akkared.format

import com.flowtomation.akkared.model._
import com.flowtomation.akkared.util.JsonReader
import org.scalatest.WordSpec
import play.api.libs.json.{JsArray, JsBoolean, JsSuccess}
import play.api.libs.json.Json._

import collection.immutable.Seq

class FlowsFormatSpec extends WordSpec {

  "The flows format" when {

    "reading" should {

      "correctly handle an empty json" in {
        val json = arr()
        val result = FlowsFormat.FlowsFormat.reads(json)
        assert(result == JsSuccess(Flows.empty))
      }

      "correctly handle a real json" in {
        val json = JsonReader.fromClasspath("flow.json")
        val JsSuccess(result, _) = FlowsFormat.FlowsFormat.reads(json)
        assert(result.items.length == 14)
      }
    }

    "writing" should {

      "correctly write an empty flows file" in {
        val flows = Flows.empty
        val result = FlowsFormat.FlowsFormat.writes(flows)
        assert(result == arr())
      }

      "correctly write multiple flows" in {
        val flows = Flows(
          FlowTab(ItemId("t1"), "tab1", disabled = true, "This is a test"),
          FlowTab(ItemId("t2"), "tab2", disabled = false, "This is a test"),
          FlowNode(ItemId("n1"), ItemId("t2"), "foo", "footype", 10, 20, Seq.empty, Map("test" -> JsBoolean(true)))
        )
        val result = FlowsFormat.FlowsFormat.writes(flows)
        assert(result.as[JsArray].value.length == 3)
      }

    }

  }
}
