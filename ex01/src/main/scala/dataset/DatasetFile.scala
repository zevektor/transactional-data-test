package it.zevektor.datatest
package dataset

import dataset.DatasetFile.deserializeValue
import util.JsonUtils
import util.Constants._

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.functions.col
import org.apache.spark.sql.types._
import org.apache.spark.sql.{DataFrame, Row, SparkSession}
import org.slf4j.{Logger, LoggerFactory}
import ujson.Value

class DatasetFile (filePath: os.Path)(implicit spark: SparkSession) {
  private val logger: Logger = LoggerFactory.getLogger(getClass)

  private val datasetData: Value = {
    logger.info(s"Reading Dataset File: ${filePath}")
    JsonUtils.readJson(filePath)
  }

  private val datasetColumns: Seq[Value] = {
    val columns = datasetData(META)(VIEW)(COLUMNS).arr
    logger.info(s"Dataset Columns: ${columns.map(_(FIELD_NAME).str).mkString(",")}")
    logger.info(s"Total Dataset Columns: ${columns.size}")
    columns
  }
  private val datasetRecords: Seq[Seq[Any]] = {
    val allRecords = datasetData(DATA).arr.map(_.arr)
    val completeRecords = allRecords.filter(_.size == datasetColumns.size)
    val invalidRecordsCount = allRecords.size - completeRecords.size
    if (invalidRecordsCount != 0)
      logger.warn(s"Dataset Records - Total: ${allRecords.size}, Invalid: ${invalidRecordsCount}, Valid: ${completeRecords.size}")
    completeRecords
      .map(_.zipWithIndex.map{case (value: Value, index: Int) => deserializeValue(value, datasetColumns(index)(DATA_TYPE_NAME).str)})
  }

  private val sparkSchema: StructType = datasetColumns.foldLeft(new StructType){
    case (schema: StructType, column: Value) => {
      val metadata = new MetadataBuilder().putString(DESCRIPTION, columnDescription(column)).build
      schema.add(StructField(column(FIELD_NAME).str, StringType, metadata = metadata))
    }
  }

  private def columnDescription(column: Value): String = {
    if (column.obj.contains(DESCRIPTION)) column(DESCRIPTION).str else "N/A"
  }

  private val sparkRecords: RDD[Row] = spark.sparkContext
    .parallelize(datasetRecords.map(record => Row.fromSeq(record)), 200)

  def toDF(ignoreMetadata: Boolean=true): DataFrame = {
    var df = spark.createDataFrame(sparkRecords, sparkSchema)
    datasetColumns
      .filter(_(DATA_TYPE_NAME).str == DatasetFieldType.NUMBER)
      .foreach(column => {
        // "Guess" the proper column type
        val columnName: String = column(FIELD_NAME).str
        val columnType: DataType =
          if (!columnName.endsWith(ID_SUFFIX) && column(FORMAT)(NO_COMMAS).str == "false") DoubleType
          else LongType
        df = df.withColumn(columnName, col(columnName).cast(columnType))
    })
    if (ignoreMetadata) {
      val metadataColumns = datasetColumns.filter(_(DATA_TYPE_NAME).str == DatasetFieldType.METADATA).map(_(FIELD_NAME).str)
      df.drop(metadataColumns:_*)
    } else {
      df
    }
  }
}

object DatasetFile {
  def apply(filePath: os.Path)(implicit spark: SparkSession): DatasetFile = {
    new DatasetFile(filePath)
  }

  private def deserializeValue(v: Value, vType: String): Any = {
    vType match {
      case DatasetFieldType.METADATA => v.toString.replace("\"","")
      case DatasetFieldType.TEXT => v.str
      case DatasetFieldType.NUMBER => v.toString.replace("\"","")
    }
  }
}
