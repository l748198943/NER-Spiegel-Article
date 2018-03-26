package data

import org.apache.spark.SparkContext
import org.apache.spark.sql.{DataFrame, SQLContext}
import org.apache.spark.sql.functions._


object NewsSource {
  
    def apply(sc: SparkContext, path: String): DataFrame = {
      val sqlContext = new SQLContext(sc)
  
      val filePath = path//"F:/ppp/kejian/bigdata/spon*.csv"
	                    
  
      val df = sqlContext.read
        .format("com.databricks.spark.csv")
        //.option("delimiter", "\t")
        //.option("quote", "^")
        .option("header", "false")
        .option("inferSchema", "true")
        .load(filePath)

      def clean = udf{s :String =>
        s.replaceAll("[/]","")
      }
    
    df.withColumn("news",clean(col("_c4")) )
    }

  
}