package com.lightbend.seldon.streamlet

import akka._
import akka.actor._
import akka.http.scaladsl._
import akka.kafka._
import akka.pattern.ask
import akka.stream.scaladsl._
import akka.util.Timeout
import cloudflow.akkastream._
import cloudflow.akkastream.scaladsl._
import cloudflow.streamlets._
import cloudflow.streamlets.avro._
import com.lightbend.seldon.actor._
import com.lightbend.seldon.executors._
import com.lightbend.seldon.stats.QueriesAkkaHttpResource
import tensorflow.modelserving.avro._

import scala.concurrent.duration._
import scala.util.Failure

class HttpFlowsServerLogicTensor(
    server:   Server,
    executor: TFBaseExecutor,
    inlet:    AvroInlet[SourceRequest],
    outlet:   AvroOutlet[ServingResult])
  (implicit context: AkkaStreamletContext) extends ServerStreamletLogic(server) {

  implicit val askTimeout: Timeout = Timeout(30.seconds)

  // Model serving actor
  val modelserver: ActorRef = system.actorOf(ModelServingActorTensor.props("recommender", executor))

  override def run(): Unit = {

    sourceWithOffsetContext(inlet).via(dataFlow).runWith(sinkWithOffsetContext(outlet))
    startServer(context, containerPort)
  }

  // Data processing
  def dataFlow: FlowWithContext[SourceRequest, ConsumerMessage.CommittableOffset, ServingResult, ConsumerMessage.CommittableOffset, NotUsed] =
    FlowWithOffsetContext[SourceRequest]
      .mapAsync(1) { record ⇒ modelserver.ask(record).mapTo[ServingResult] }

  def startServer(
      context: AkkaStreamletContext,
      port:    Int): Unit = {
    val routes = QueriesAkkaHttpResource.storeRoutes(modelserver)

    Http().bindAndHandle(routes, "0.0.0.0", port)
      .map { binding ⇒
        context.signalReady()
        system.log.info(s"Bound to ${binding.localAddress.getHostName}:${binding.localAddress.getPort}")
        // this only completes when StreamletRef executes cleanup.
        context.onStop { () ⇒
          system.log.info(s"Unbinding from ${binding.localAddress.getHostName}:${binding.localAddress.getPort}")
          binding.unbind().map(_ ⇒ Dun)
        }
        binding
      }
      .andThen {
        case Failure(cause) ⇒
          system.log.error(cause, s"Failed to bind to $port.")
          context.stop()
      }
    ()
  }
}

