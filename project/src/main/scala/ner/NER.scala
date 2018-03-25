package ner


trait NER {

  def process(string: String): List[String]
}


