package it.zevektor.datatest
package util

object JsonUtils {
  def readJson(path: os.Path): ujson.Value = {
    val jsonString: String = os.read(path)
    val jsonData = ujson.read(jsonString)
    jsonData
  }
}
