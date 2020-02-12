package com.lightbend.seldon

import com.lightbend.seldon.executors._
import org.scalatest.FlatSpec
import pipelines.examples.modelserving.recommender.avro._

class SeldonTFRESTExecutorTest extends FlatSpec {

  // the model's name.
  val modelName = "recommender"
  val path = "http://localhost:8003/seldon/seldon/rest-tfserving/v1/models/recommender/:predict"

  val products = Seq(1L, 2L, 3L, 4L)

  val input = new RecommenderRecord(10L, products)

  "Processing of model" should "complete successfully" in {

    val executor = new SeldonTFRESTExecutor(modelName, path)
    println("Model created")
    val result = executor.score(input)
    println(result)
  }
}