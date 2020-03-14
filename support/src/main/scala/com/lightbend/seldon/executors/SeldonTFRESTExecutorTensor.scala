package com.lightbend.seldon.executors

import scalaj.http._
import tensorflow.modelserving.avro._
import com.lightbend.seldon.converters.TensorConverter._

class SeldonTFRESTExecutorTensor(modelName: String, signature: String, source: String) extends TFBaseExecutor(modelName, source) {

  override def invokeModel(record: SourceRequest): Either[String, ServingOutput] = {

    // Build request json
    val request = avroToJSON(signature, record.inputRecords.inputs)

    try {
      val response = Http(source).postData(request).header("content-type", "application/json").asString
      response.code match {
        case code if code == 200 ⇒ // Got successful response
          val map = JSONToAvro(record.modelResults.outputs, response.body)
          Right(ServingOutput(map))
        case _ ⇒ // Got error response
          Left(response.body)
      }
    } catch {
      case t: Throwable ⇒
        println(s"Error accessing HTTP server $source")
        t.printStackTrace()
        Left(t.getMessage)
    }
  }

  /**
   * Cleanup when a model is not used anymore. Nothing to do in this case
   */
  override def cleanup(): Unit = {}
}
