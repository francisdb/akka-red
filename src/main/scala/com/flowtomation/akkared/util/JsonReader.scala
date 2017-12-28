package com.flowtomation.akkared.util

import java.io.InputStream

import play.api.libs.json.{JsValue, Json}

import scala.util.control.NonFatal


object JsonReader {

  def fromClasspath(name: String): JsValue = {
    withInputStream(getClass.getClassLoader.getResourceAsStream(name))(Json.parse)
  }

  private def withInputStream[T](is: InputStream)(block: InputStream => T): T ={
    try{
      block(is)
    }finally{
      try{
        is.close()
      }catch{
        case NonFatal(e) => //ignore
      }
    }
  }
}
