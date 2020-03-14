package com.lightbend.seldon

import com.lightbend.seldon.converters.TensorConverter._
import org.scalatest.FlatSpec
import tensorflow.serving.predict.PredictResponse
import tensorflow.support.avro._

class TensorConvertertest  extends FlatSpec {

  private val products = Seq(1L, 2L, 3L, 4L)
  private val boolean = Seq(true, true, false, false)
  private val string = Seq("one", "two", "three", "four")
  private val user = 10L
  private val dtype = DataType.DT_FLOAT
  private val shape = TensorShape(Seq(Dim(products.size.toLong, ""), Dim(1L, "")))
  private val pTensor = Tensor(dtype = dtype, tensorshape = shape, float_data = Some(products.map(_.toFloat)))
  private val uTensor = Tensor(dtype = dtype, tensorshape = shape, float_data = Some(products.map(_ => user.toFloat)))
  private val shape1 = TensorShape(Seq(Dim(products.size.toLong, "")))
  private val pTensor1 = Tensor(dtype = dtype, tensorshape = shape1, float_data = Some(products.map(_.toFloat)))
  private val bTensor = Tensor(dtype = DataType.DT_BOOL, tensorshape = shape, boolean_data = Some(boolean))
  private val sTensor = Tensor(dtype = DataType.DT_STRING, tensorshape = shape, string_data = Some(string))

  private val rTensor = Tensor(dtype = dtype, tensorshape = shape)
  private val message = """{"outputs":[["0.01"],["0.02"],["0.03"],["0.04"]]}"""
  private val message1 = """{"outputs":["0.01","0.02","0.03","0.04"]}"""
  private val message2 = """{"outputs":[[["0.01"],["0.02"]],[["0.03"],["0.04"]]]}"""

  "Converting feom Avro to FSON" should "should work correctly" in {
    println(avroToJSON("", Map("users" -> uTensor, "products" -> pTensor)))
    println(avroToJSON("", Map("products" -> pTensor1)))
  }

  "Converting feom JSON to Avro" should "should work correctly" in {
    println(JSONToAvro(Map("outputs" -> rTensor), message))
    println(JSONToAvro(Map("outputs" -> rTensor), message1))
    println(JSONToAvro(Map("outputs" -> rTensor), message2))
  }

  "Converting feom Avro to Proto" should "should work correctly" in {
    val proto = avroToProto("recommender","", Map("users" -> uTensor, "products" -> pTensor))
    println(proto)

  }

  "Converting from Proto to Avro" should "should work correctly" in {
    val proto = avroToProto("recommender","", Map("users" -> uTensor, "products" -> pTensor))
    val prediction = PredictResponse(outputs=proto.inputs)
    println(protoToAvro(prediction))
  }

  "Converting feom Avro to Tensors" should "should work correctly" in {
    println(avroToTensor(Map("users" -> uTensor, "products" -> pTensor)))
    println(avroToTensor(Map("boolean" -> bTensor)))
    println(avroToTensor(Map("string" -> sTensor)))
  }

  "Converting from Tensors to Avro" should "should work correctly" in {
    val floatTensor = org.tensorflow.Tensor.create(Array( Array(1.0f, 2.0f, 3.0f), Array(4.0f, 5.0f, 6.0f)))
    val boolTensor = org.tensorflow.Tensor.create(Array( Array(true, false, true), Array(false, true, false)))
    val stringTensor = avroToTensor(Map("string" -> sTensor))
    println(tensorToAvro(Map("float" -> floatTensor)))
    println(tensorToAvro(Map("boolean" -> boolTensor)))
    println(tensorToAvro(stringTensor))
  }
}
