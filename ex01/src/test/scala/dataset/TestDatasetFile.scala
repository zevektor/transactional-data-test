package it.zevektor.datatest
package dataset

import org.apache.spark.sql.types.{DoubleType, LongType, StringType}
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.scalatest.{BeforeAndAfterAll, FunSuite}

class TestDatasetFile extends FunSuite with BeforeAndAfterAll {

  implicit var spark: SparkSession = _
  var datasetFile: DatasetFile = _

  val METADATA_FIELD_INDEX: Int = 0
  val ID_FIELD_INDEX: Int = 0
  val TEXT_FIELD_INDEX: Int = 1
  val NUMBER_FIELD_INDEX: Int = 2

  override def beforeAll(): Unit = {
    spark = SparkSession.builder.master("local[*]").getOrCreate
    val filePath: os.Path = os.pwd / "src" / "test" / "resources" / "test-dataset.json"
    datasetFile = DatasetFile(filePath)
  }

  override def afterAll(): Unit = {
    spark.stop()
  }

  test("DatasetFile keep metadata columns") {
    val keepMetadataDF: DataFrame = datasetFile.toDF(ignoreMetadata = false)
    assert(keepMetadataDF.columns.contains(":metadatafield"))
  }

  test("DatasetFile doesn't keep metadata columns by default") {
    val keepMetadataDF: DataFrame = datasetFile.toDF()
    assert(!keepMetadataDF.columns.contains(":metadatafield"))
  }

  test("DatasetFile removes corrupted records") {
    val datasetDF = datasetFile.toDF()
    // The incomplete row has been removed
    assert(datasetDF.count == 2)
  }

  test("DatasetFile metadata fields are converted to string") {
    val datasetDF = datasetFile.toDF(ignoreMetadata = false)
    assert(datasetDF.schema.fields(METADATA_FIELD_INDEX).dataType == StringType)
  }

  test("DatasetFile IDs are converted to long") {
    val datasetDF = datasetFile.toDF()
    assert(datasetDF.schema.fields(ID_FIELD_INDEX).dataType == LongType)
  }

  test("DatasetFile text columns are converted to string") {
    val datasetDF = datasetFile.toDF()
    assert(datasetDF.schema.fields(TEXT_FIELD_INDEX).dataType == StringType)
  }

  test("DatasetFile number columns are converted to doubles") {
    val datasetDF = datasetFile.toDF()
    assert(datasetDF.schema.fields(NUMBER_FIELD_INDEX).dataType == DoubleType)
  }

}
