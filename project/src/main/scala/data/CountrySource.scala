package data

import org.apache.spark.SparkContext
import org.apache.spark.sql.{DataFrame, SQLContext}
import org.apache.spark.sql.functions._


object CountrySource {
  
    def apply(sc: SparkContext, path: String): DataFrame = {
      val sqlContext = new SQLContext(sc)
  
      val filePath = path
  
      val df = sqlContext.read
        .format("com.databricks.spark.csv")
        .option("delimiter", "\t")
        //.option("quote", "^")
        .option("header", "true")
        .option("inferSchema", "true")
        .load(filePath)
    
    val CleanReview = udf((ss: String) => {
      val cString = ss.replaceAll("\\(.*\\)", "").replaceAll("[\\- ]", "")
      val regex = "[^a-zA-Z]"
      cString//.replaceAll(regex, " ")
    })

    val toWordArray = udf((cell: String) => {
      cell.toLowerCase
          .replaceAll("[,]", "").replaceAll("[\\-\\/]", " ").split(" ")
          .filter(x => !x.equals("der") && !x.equals("die"))
    })

    df.withColumn("Country", CleanReview(col("Country"))).withColumn("filtered_w", toWordArray(col("PeopleOrAdjective")))
    }

  
}