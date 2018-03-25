package ner

import edu.stanford.nlp.ie.crf.CRFClassifier
import edu.stanford.nlp.ling.{CoreAnnotations, CoreLabel}
import edu.stanford.nlp.pipeline.DefaultPaths
import scala.collection.mutable.ListBuffer

class StanfordNER extends NER {

  private[this] val classifier = CRFClassifier
  .getClassifierNoExceptions("edu/stanford/nlp/models/ner/german.conll.germeval2014.hgc_175m_600.crf.ser.gz")

  def process(string: String): List[String] = {

    val keywords = new ListBuffer[(String, String)]


    val dp = classifier.classify(string).iterator()

    while (dp.hasNext) {

      def recognized(word: CoreLabel): Boolean = {
        !word.get(classOf[CoreAnnotations.AnswerAnnotation]).equals("O") &&
          !word.get(classOf[CoreAnnotations.AnswerAnnotation]).equals("")
      }

      
      def annotation(word: CoreLabel) = word.get(classOf[CoreAnnotations.AnswerAnnotation])

      
      val words = dp.next().iterator
      var flag = 0
      val filteredWords = ListBuffer[CoreLabel]()

      while(words.hasNext) {
        val i = 1
        val word = words.next.asInstanceOf[CoreLabel]
        val word_text = word.word
        if(recognized(word)) {
          flag = flag +1
          filteredWords += word
        }
        /*
         * In order to avoid some missing words, 
         * at most 2 nouns(begin with upper case) after a entity are also
         * recognized as potential entities.
         */
        else{
          if(word_text.charAt(0).isUpper&&flag > 0){
          filteredWords += word
          flag = flag +1 
          }
          else if(!word_text.charAt(0).isUpper|| flag >3){
            flag = 0
          }
        }
      }

      val wordsNER = filteredWords.map(word => ((word.word,annotation(word)),(word.beginPosition, word.endPosition)))
      val chunks = new ListBuffer[((String, String),(Int, Int))]
      
      for(i <- 0 to wordsNER.length -1) {
        if(i>0){


            if((chunks.last._2._2+1 == wordsNER(i)._2._1
              //  &&chunks.last._2._2 +2 >= wordsNER(i)._2._1
                && (chunks.last._1._2 == wordsNER(i)._1._2 || 
                    (chunks.last._1._2.equals("PERSON")&&wordsNER(i)._1._2.equals("O")))) )
            {  chunks += ((((chunks.last._1._1 + " " + wordsNER(i)._1._1, chunks.last._1._2)),
                           ((chunks.last._2._1,wordsNER(i)._2._2))))
               if(i == wordsNER.length -1){
                  keywords += chunks.last._1
               }
            }
            else{           
              keywords +=chunks.last._1
              chunks += wordsNER(i)
               if(i == wordsNER.length -1){
                  keywords += chunks.last._1
               }
            }
          

        }
        else{
          chunks += wordsNER(i)
          if(i == wordsNER.length -1){
                  keywords += chunks.last._1
          }
        } 
      }
    }

    keywords.map(x => (x._1+"&&"+ x._2)).toList
  }
}