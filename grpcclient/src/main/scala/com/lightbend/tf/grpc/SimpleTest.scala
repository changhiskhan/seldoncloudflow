package com.lightbend.tf.grpc

import io.grpc._
import io.grpc.stub.MetadataUtils
import org.tensorflow.framework.tensor._
import org.tensorflow.framework.tensor_shape._
import org.tensorflow.framework.types._
import tensorflow.serving.model._
import tensorflow.serving.predict._
import tensorflow.serving.prediction_service._

import scala.language.existentials

object SimpleTest {

  val SELDON_KEY = Metadata.Key.of("seldon", Metadata.ASCII_STRING_MARSHALLER)
  val NAMESPACE_KEY = Metadata.Key.of("namespace", Metadata.ASCII_STRING_MARSHALLER)

  def main(args: Array[String]): Unit = {

    val host = "localhost"
    //    val port = 8500
    val port = 8003
    // the model's name.
    val modelName = "recommender"

    val products = Seq(1L, 2L, 3L, 4L)
    val users = 20L

    // Headers
    val headers = new Metadata()
    headers.put(SELDON_KEY, "grpc-tfserving")
    headers.put(NAMESPACE_KEY, "seldon")

    // create a stub
    val channelbuilder = ManagedChannelBuilder.forAddress(host, port)
    channelbuilder.usePlaintext()
    val server = PredictionServiceGrpc.blockingStub(channelbuilder.build())
    val serverWithHeaders = MetadataUtils.attachHeaders(server, headers)

    // Model spec
    val model = ModelSpec(name = modelName, signatureName = "serving_default")

    0.to(10).foreach { _ ⇒
      val start = System.currentTimeMillis()
      // Build products and users proto
      val tensorshape = Some(TensorShapeProto(Seq(TensorShapeProto.Dim(products.size.toLong), TensorShapeProto.Dim(1l))))
      val productProto = TensorProto(dtype = DataType.DT_FLOAT, tensorShape = tensorshape, floatVal = products.map(_.toFloat))
      val userProto = TensorProto(dtype = DataType.DT_FLOAT, tensorShape = tensorshape, floatVal = products.map(_ ⇒ users.toFloat))

      // Create request
      val request = PredictRequest(modelSpec = Some(model), inputs = Map("products" -> productProto, "users" -> userProto))
      //    println(s"Request ${request.toString}")

      // run predict
      try {
        val response = serverWithHeaders.predict(request)
        //    println(s"Responce ${response.toString}")
        val probabilities = response.outputs.get("predictions").get.floatVal
        //    println(s"probabilities $probabilities")
        val predictions = probabilities.zip(products).map(r ⇒ (r._2.toString, r._1)).unzip
        val time = System.currentTimeMillis() - start
        println(s"predictions $predictions in $time ms")
      } catch {
        case t: Throwable ⇒
          t.printStackTrace()
      }
    }
  }
}
