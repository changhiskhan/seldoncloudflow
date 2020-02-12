package com.lightbend.seldon.grpc

import cloudflow.akkastream._
import cloudflow.streamlets._
import cloudflow.streamlets.avro._
import com.lightbend.seldon.executors._
import com.lightbend.seldon.streamlet.HttpFlowsServerLogic
import pipelines.examples.modelserving.recommender.avro._

class GRPCModelServerStreamlet extends AkkaServerStreamlet {

  val in = AvroInlet[RecommenderRecord]("recommender-records")
  val out = AvroOutlet[RecommenderResult]("recommender-results", _.inputRecord.datatype)

  final override val shape = StreamletShape.withInlets(in).withOutlets(out)

  val executor = new SeldonTFGRPCExecutor("recommender", "localhost", 8003)

  override protected def createLogic(): AkkaStreamletLogic =
    new HttpFlowsServerLogic(this, executor, in, out)
}
