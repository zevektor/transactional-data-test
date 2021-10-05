package it.zevektor.datatest
import dataset.DatasetFile
import util.SparkUtils

import org.apache.spark.sql.SparkSession

object MainApp {

  def main(args: Array[String]): Unit = {
    implicit val spark: SparkSession = SparkUtils.getSparkSession
    try {
      val inputPath = os.root / "input" / "input_dataset.json"
      val datasetFile = DatasetFile(inputPath)

      val df = datasetFile.toDF(ignoreMetadata = true)
      df.printSchema

      val measuresTable = df.select("measureid", "measurename", "measuretype").distinct
      SparkUtils.writeDataFrameToDb(measuresTable, "measures")

      val dataTable = df.select("measureid", "stratificationlevel", "statename", "countyname", "reportyear", "value", "unit", "unitname", "dataorigin", "monitoronly")
      SparkUtils.writeDataFrameToDb(dataTable, "air_quality_reports")

    } finally {
      spark.stop()
    }
  }

}
