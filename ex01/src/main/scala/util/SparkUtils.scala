package it.zevektor.datatest
package util

import org.apache.spark.sql.types.{DataType, LongType, StringType}
import org.apache.spark.sql.{DataFrame, SaveMode, SparkSession}

import java.util.Properties

object SparkUtils {
  def getSparkSession: SparkSession = SparkSession.builder.getOrCreate

  def getColumnType(typeString: String): DataType = typeString match {
    case "meta_data" => StringType
    case "text" => StringType
    case "number" => LongType
    case _ => StringType
  }

  def writeDataFrameToDb(output: DataFrame, tableName: String, mode: SaveMode = SaveMode.Overwrite): Unit = {
    val dbUser: String = sys.env.getOrElse("POSTGRES_USER", "")
    val dbPassword: String = sys.env.getOrElse("POSTGRES_PASSWORD", "")
    val dbHost: String = sys.env.getOrElse("POSTGRES_HOST", "")

    val connectionProperties = new Properties()
    connectionProperties.put("user", dbUser)
    connectionProperties.put("password", dbPassword)
    connectionProperties.put("driver", "org.postgresql.Driver")

    output.write.mode(mode)
      .jdbc(
        url=s"jdbc:postgresql://${dbHost}:5432/",
        table=tableName,
        connectionProperties
      )
  }
}
