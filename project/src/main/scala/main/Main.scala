package main

import main.functions._
import data._
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.functions._
import org.apache.spark.ml.classification.RandomForestClassifier
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator
import org.apache.spark.ml.feature.{CountVectorizer, CountVectorizerModel, IndexToString, StringIndexer}
import org.apache.spark.ml.Pipeline
import edu.stanford.nlp.io.IOUtils;
import org.apache.spark.sql.SQLContext

import org.apache.log4j.BasicConfigurator;

import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.pipeline.Annotation;
//import edu.stanford.nlp.pipeline.CoreDocument;
//import edu.stanford.nlp.pipeline.CoreSentence;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

import java.io.IOException
import java.util.Properties
import scala.collection.mutable.WrappedArray
import org.apache.spark.rdd._
import scala.collection.mutable.ListBuffer

object Main extends App{
  override def main (args: Array[String]) {
    super.main(args)

    if (args.length < 3) {
      println("Please add the path of files as argument")
      //return
    }
    val news_input_path = "./data/spon-20150601_20150701.csv"//args(0)//
    val dic_input_path = "./data/country_2.csv"//args(1)//
    val output_path =""//args(2)// 

    val conf = new SparkConf().setAppName("nlpDemo").setMaster("local[*]")
    val sc = new SparkContext(conf)
    val sqlContext = new SQLContext(sc)
    
    import sqlContext.implicits._

    //var df = DataSource(sc,input_path)
    var df = NewsSource(sc,news_input_path)
    val country_list = CountrySource(sc,dic_input_path)//.rdd.map(x=> (x.getInt(1),x.getString(0))).lookup

    
    def colAdd = udf{(a:WrappedArray[String], b: String) =>
      val slist = ListBuffer[String]()
      for(x <- a){
        slist += x
        
      }
      slist += b.toLowerCase
      slist
    }
    
    def suffix_s = udf{ a:WrappedArray[String] =>
      val slist = ListBuffer[String]()
      for(x <- a){
        if(x.length>0){
          val word = x.split("&&")(0)
          val cate = x.split("&&")(1)
          if(word.length>0 && cate.length>0){
              slist += x
              if(word.charAt(0).isUpper && word.endsWith("s"))
                slist += word.dropRight(1)+"&&"+cate
          }

        }
      }
      slist.toList
    }
    val country_dic = 
    country_list
    .select('Country, colAdd('filtered_w, 'Country).as('all))
    .select('Country, explode('all).as('exp))
    .rdd.map(x => (x.getString(1), x.getString(0))).collect.toMap

    def udfCountryLemma =  udf{ sq: WrappedArray[String] =>
      val slist = ListBuffer[String]()
      for(word <- sq){
        val w = word.split("&&")(0).toLowerCase
        val options = country_dic.get(w)
        val suffix = List("e", "er", "en", "es", "s", "ern", "nen")
        if(!options.isEmpty){
          slist += options.head
        }
        else{
          for(sfx <- suffix){
            if(w.endsWith(sfx)){
              val s_options = country_dic.get(w.dropRight(sfx.length))
              if(!s_options.isEmpty)
                slist += s_options.head
            }
          }
        }
      }
     
     slist.toList
    }
    def findCountryLemma(s: String, dic: RDD[(String,String)]): String ={
      var rt =""
      if(s.length>0)
        rt = dic.lookup(s)(0)
      rt
    }
    

    //.foreach(println)
      //.select('Country, 'exp, explode('Adjective))
    //df.select('Title).show
    val output = df
    .select('_c0,'_c3, explode(ssplit('_c4)).as('sen))
    .select('_c0,'_c3, 'sen, tokenize('sen).as('words), ner('sen).as('ner))
    .select(toHash('_c0).as('title_hash), ner('_c0).as('title_ner), suffix_s('ner).as('ref))
    .select('title_hash,'title_ner, 'ref, udfCountryLemma('ref).as('long))
    
    //output.rdd.saveAsTextFile(output_path)
    output.show(125,false)
    
    
    
    
    //.select(cleanxml('News).as('article))
    //.select('Title, explode(ssplit('News)).as('sen))
    //.select('sen, tokenize('sen).as('words), ner('sen).as('ner), ner('Title).as('title_ner))
    //.select('title_ner, 'ner)
   // .show(25,false)
    //.foreach(println)
   // output.write.format("com.databricks.spark.csv").save(output_path)
  // output.printSchema
    
  }
}