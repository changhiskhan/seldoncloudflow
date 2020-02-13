package com.lightbend.seldon.rest

import cloudflow.akkastream._
import cloudflow.streamlets._
import cloudflow.streamlets.avro._
import com.lightbend.seldon.executors._
import com.lightbend.seldon.streamlet.HttpFlowsServerLogic
import pipelines.examples.modelserving.recommender.avro._

class RESTModelServerStreamlet extends AkkaServerStreamlet {

  val in = AvroInlet[RecommenderRecord]("recommender-records")
  val out = AvroOutlet[RecommenderResult]("recommender-results", _.inputRecord.datatype)

  final override val shape = StreamletShape.withInlets(in).withOutlets(out)

  //  val path = "http://localhost:8003/seldon/seldon/rest-tfserving/v1/models/recommender/:predict" // local
  val path = "http://rest-tfserving-resttfserving-model.seldon.svc.cluster.local:8000/v1/models/recommender/:predict" // cluster

  val executor = new SeldonTFRESTExecutor("recommender", path)

  override protected def createLogic(): AkkaStreamletLogic =
    new HttpFlowsServerLogic(this, executor, in, out)
}
