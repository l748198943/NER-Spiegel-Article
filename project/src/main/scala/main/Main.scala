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
    val news_input_path = "J:/ba/news-2016/spon-2016*"//args(0)//
    val dic_input_path = "./data/country_2.csv"//args(1)//
    val output_path ="./data/allEn2016/all"//args(2)// 

    val conf = new SparkConf().setAppName("nlpDemo").setMaster("local[*]")
    val sc = new SparkContext(conf)
    val sqlContext = new SQLContext(sc)
    
    import sqlContext.implicits._

    //var df = DataSource(sc,input_path)
    var df = NewsSource(sc,news_input_path)
    val country_list = CountrySource(sc,dic_input_path)//.rdd.map(x=> (x.getInt(1),x.getString(0))).lookup

    
    def colAddString = udf{(a:WrappedArray[String], b: String) =>
      val slist = ListBuffer[String]()
      for(x <- a){
        slist += x
        
      }
      slist += b.toLowerCase
      slist
    }
    
    def colAddStringList = udf{(a:WrappedArray[String], b: WrappedArray[String]) =>
      val slist = ListBuffer[String]()
      for(x <- a){
        slist += x    
      }
      for(x <- b){
        slist += x    
      }
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
    .select('Country, colAddString('filtered_w, 'Country).as('all))
    .select('Country, explode('all).as('exp))
    .rdd.map(x => (x.getString(1), x.getString(0))).collect.toMap

    def udfCountryLemma =  udf{ sq: WrappedArray[String] =>
      val slist = ListBuffer[String]()
      for(word <- sq){
        val w = word.split("&&")(0).toLowerCase
        val options = country_dic.get(w)
        val suffix = List("e", "er", "en", "es", "s", "ern", "nen")
        if(!options.isEmpty){
          slist += options.head+"&&LOC"
        }
        else{
          for(sfx <- suffix){
            if(w.endsWith(sfx)){
              val s_options = country_dic.get(w.dropRight(sfx.length))
              if(!s_options.isEmpty)
                slist += s_options.head+"&&LOC"
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
    def lineSummary = udf{(title: String, time: String, a:WrappedArray[String], b: WrappedArray[String],  c: WrappedArray[String]) =>
      var slist = ""
      for(x <- a){
        slist = slist +"\t"+x.split("&&")(0) 
        slist = slist + "&&" +title.hashCode + "&&" + time
      }
      for(x <- b){
         slist = slist +"\t"+x.split("&&")(0)
         slist = slist +"&&" + title.hashCode + "&&" + time
      }
      for(x <- c){
         slist = slist +"\t"+x.split("&&")(0)
         slist = slist + "&&" +title.hashCode + "&&" + time
      }
      slist
    }    

    //.foreach(println)
      //.select('Country, 'exp, explode('Adjective))
    //df.select('Title).show
    val output = df
    .select('_c0, '_c3, explode(ssplit('news)).as('sen))
    .select('_c0, '_c3, 'sen, tokenize('sen).as('words), ner('sen).as('ner))
    .select('_c0,  '_c3, ner('_c0).as('title_ner), suffix_s('ner).as('ref))
    .select('_c0, '_c3, suffix_s('title_ner).as('title_ref), 'ref, udfCountryLemma('ref).as('long))
    .select(lineSummary('_c0, '_c3, 'title_ref, 'ref ,'long))
  //  df.show(100)
   // df.rdd.map(x => (x.getString(0),x.getString(4))).filter(_._2.length==0).foreach(println)

    
    output.rdd
    .map(x => (x.getString(0)))
    .flatMap(_.split("\t"))
    .filter(_.length>0)
   // .foreach(println)
    .saveAsTextFile(output_path)
    //.foreach(println)
    //.map(x=>(x.split("&&")(0),(x.split("&&")(1).toInt, x.split("&&")(2).toLong)))
    //.filter(_._1.startsWith("Ungewöhnliche Reise-Apps"))
    //.reduceByKey((x1, x2) => (x1 ++ x2))
    //.map(x => (x._1, x._2.distinct))
    //.foreach(println)
    //output.show(125,false)
    
    
    
    
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