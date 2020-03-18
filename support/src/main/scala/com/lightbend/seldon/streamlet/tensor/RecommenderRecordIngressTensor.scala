package com.lightbend.seldon.streamlet.tensor

import akka.NotUsed
import akka.stream.scaladsl._
import cloudflow.akkastream._
import cloudflow.akkastream.scaladsl._
import cloudflow.streamlets._
import cloudflow.streamlets.avro._
import tensorflow.modelserving.avro._
import tensorflow.support.avro._

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._
import scala.util.Random

/**
 * Ingress of data for recommendations. In this case, every @dataFrequencyMilliseconds we
 * load and send downstream one record that is randomly generated.
 */
final case object RecommenderRecordIngressTensor extends AkkaStreamlet {

  // Output
  val out = AvroOutlet[SourceRequest]("source-records", _.datatype)

  // Shape
  final override val shape = StreamletShape.withOutlets(out)

  // Create Logic
  override final def createLogic = new RunnableGraphStreamletLogic {
    // Runnable graph
    def runnableGraph =
      RecordIngressUtilsTensor.makeSource().to(plainSink(out))
  }
}

/**
 * An object generating a stream of input records for recommendations
 */
object RecordIngressUtilsTensor {
  // Data frequency
  lazy val dataFrequencyMilliseconds: FiniteDuration = 1.millisecond // 1000 msg per sec

  // Make source
  def makeSource(frequency: FiniteDuration = dataFrequencyMilliseconds): Source[SourceRequest, NotUsed] = {
    Source.repeat(RecordGeneratorTensor)
      .map(gen ⇒ gen.generateRecord())
      .throttle(1, frequency)
  }
}

/**
 * An object generating individual recommendation's records
 */
private object RecordGeneratorTensor {

  // Random generator
  protected lazy val generator = Random
  // Data type
  private val dtype = DataType.DT_FLOAT
  // Input/Output names
  private val inputs = Seq("users", "products")
  private val outputs = "predictions"

  // Generate new request record
  def generateRecord(): SourceRequest = {
    // User
    val user = generator.nextInt(1000).toLong
    // Number of products
    val nprods = generator.nextInt(30)
    // Products
    val products = new ListBuffer[Long]()
    0 to nprods - 1 foreach { _ ⇒ products += generator.nextInt(300).toLong }
    // Shape
    val shape = TensorShape(Seq(Dim(nprods.toLong, ""), Dim(1L, "")))
    // Tensors
    val pTensor = Tensor(dtype = dtype, tensorshape = shape, float_data = Some(products.map(_.toFloat)))
    val uTensor = Tensor(dtype = dtype, tensorshape = shape, float_data = Some(products.map(_ ⇒ user.toFloat)))
    val rTensor = Tensor(dtype = dtype, tensorshape = shape)
    // Source record
    SourceRequest(
      inputRecords = SourceRecord(Map(inputs(0) -> uTensor, inputs(1) -> pTensor)),
      modelResults = ServingOutput(Map(outputs -> rTensor)))
  }
}
