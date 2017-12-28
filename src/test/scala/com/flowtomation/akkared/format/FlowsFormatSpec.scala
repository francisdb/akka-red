package com.flowtomation.akkared.format

import com.flowtomation.akkared.model.{Flow, ItemId, Node, Tab}
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
        assert(result == JsSuccess(Seq.empty))
      }

      "correctly handle a real json" in {
        val json = JsonReader.fromClasspath("flow.json")
        val JsSuccess(result, _) = FlowsFormat.FlowsFormat.reads(json)
        assert(result.length == 2)
      }
    }

    "writing" should {

      "correctly write an empty flows file" in {
        val flows = Seq.empty[Flow]
        val result = FlowsFormat.FlowsFormat.writes(flows)
        assert(result == arr())
      }

      "correctly write multiple flows" in {
        val flows = Seq(
          Flow(
            Tab(ItemId("t1"), "tab1", disabled = true, "This is a test"),
            Seq.empty
          ),
          Flow(
            Tab(ItemId("t1"), "tab1", disabled = false, "This is a test"),
            Seq(
              Node(ItemId("n1"), "foo", "footype", 10, 20, Seq.empty, Map("test" -> JsBoolean(true)))
            )
          )
        )
        val result = FlowsFormat.FlowsFormat.writes(flows)
        assert(result.as[JsArray].value.length == 3)
      }

    }

  }
}
