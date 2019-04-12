package speechrecognizer

import java.io.{File, FileInputStream}

import edu.cmu.sphinx.api.{Configuration, SpeechResult, StreamSpeechRecognizer}


class VoiceRecognizer() {
//  val configuration = new Configuration()
//
//  configuration.setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us")
//  configuration.setDictionaryPath("resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict")
//  configuration.setLanguageModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us.lm.bin")
//
//  val recognizer = new StreamSpeechRecognizer(configuration)
//
//  def transcribeFile(path: String) {
//    val stream = new FileInputStream(new File(path))
//
//    recognizer.startRecognition(stream)
//    var result: SpeechResult = null
//    while ({(result = recognizer.getResult) != null}) {
//      System.out.format("Hypothesis: %s\n", result.getHypothesis)
//    }
//    recognizer.stopRecognition()
//  }
}
