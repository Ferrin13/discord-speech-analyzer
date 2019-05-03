package speechrecognizer

import java.io._
import java.util.Collections

import com.google.api.client.util.Charsets
import com.google.common.io.CharStreams
import edu.stanford.nlp.simple.{Document, Token}

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContextExecutor, Future}

object TranscriptionAnalyzer {
  implicit val executor: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global

  private final val TRANSCRIPTION_FILE_EXTENSION = "txt"

  //From https://www.ranks.nl/stopwords
  val LONG_STOP_WORDS = List("a","about","above","after","again","against","all","am","an","and","any","are","aren't","as",
    "at","be","because","been","before","being","below","between","both","but","by","can't","cannot","could","couldn't",
    "did","didn't","do","does","doesn't","doing","don't","down","during","each","few","for","from","further","had",
    "hadn't","has","hasn't","have","haven't","having","he","he'd","he'll","he's","her","here","here's","hers","herself",
    "him","himself","his","how","how's","i","i'd","i'll","i'm","i've","if","in","into","is","isn't","it","it's","its",
    "itself","let's","me","more","most","mustn't","my","myself","no","nor","not","of","off","on","once","only","or",
    "other","ought","our","ours	ourselves","out","over","own","same","shan't","she","she'd","she'll","she's","should",
    "shouldn't","so","some","such","than","that","that's","the","their","theirs","them","themselves","then","there",
    "there's","these","they","they'd","they'll","they're","they've","this","those","through","to","too","under","until",
    "up","very","was","wasn't","we","we'd","we'll","we're","we've","were","weren't","what","what's","when","when's",
    "where","where's","which","while","who","who's","whom","why","why's","with","won't","would","wouldn't","you",
    "you'd","you'll","you're","you've","your","yours","yourself","yourselves")

  val SHORT_STOP_WORDS = List("a", "an", "and", "are", "as", "at", "be", "but", "by",
    "for", "if", "in", "into", "is", "it",
    "no", "not", "of", "on", "or", "such",
    "that", "the", "their", "then", "there", "these",
    "they", "this", "to", "was", "will", "with")

  val SWEAR_WORDS = List("fucking", "fuck", "shit", "ass", "asshole", "bitch", "cunt", "cock", "dick", "damn", "bullshit", "horseshit", "dogshit")

  def userTranscriptionData(channelName: String, userId: String): Future[TranscriptionData] = {
    val files = new File(Utils.userCombinePath(channelName, userId)).listFiles()
      .map(_.getPath)
      .filter(_.takeRight(3) == TRANSCRIPTION_FILE_EXTENSION)
    val text = combineTranscriptionFiles(files.toList)
    userTranscriptionData(userId, new Document(text))
  }

  def combineTranscriptionFiles(fileNames: List[String]): String = {
    val stream = new SequenceInputStream(Collections.enumeration(fileNames.map(new FileInputStream(_)).asJava))
    CharStreams.toString(new InputStreamReader(stream, Charsets.UTF_8))
  }

  private def userTranscriptionData(userId: String, document: Document) : Future[TranscriptionData] = Future {
    val tokens = getTokens(document).filter{ t =>
      t.word().length match {
        case 1 => t.word().matches("[ai]")
        case 2 => !t.word().matches("'s")
        case _ => true
      }
    }
    val lemmas = tokens.map(_.lemma())
      .groupBy(s => s)
      .map{ case (s, group) => (s, group.length) }.toList

    TranscriptionData(
      userId,
      tokens.map(_.word()),
      lemmas
    )
  }

  def standaloneTranscriptionAnalysis(transcriptionData: TranscriptionData): StandaloneTranscriptionAnalysis =  {
    val words = transcriptionData.words
    val lemmas = transcriptionData.groupedLemmas
    val lemmasNoStop = lemmas.filter{case (s, _) => !TranscriptionAnalyzer.LONG_STOP_WORDS.contains(s.toLowerCase)}

    val novelWordPercentage = lemmas.length / words.length.toDouble
    val novelWordNoStopPercentage = lemmasNoStop.length / lemmasNoStop.map(t => t._2).sum.toDouble
    val swearPercentage = lemmas.filter{ case (s, _) => SWEAR_WORDS.contains(s) }.map(_._2).sum / words.length.toDouble
    val avgWordLength = words.map(_.length).sum / words.length.toDouble
    StandaloneTranscriptionAnalysis(
      transcriptionData.userId,
      words.length,
      novelWordPercentage,
      novelWordNoStopPercentage,
      swearPercentage,
      avgWordLength
    )
  }

  def contextualTranscriptionAnalysis(
     transcriptionData: TranscriptionData,
     contextualData: List[TranscriptionData]
  ): ContextualTranscriptionAnalysis = {
    val otherLemmas = contextualData.filter(tData => tData.userId != transcriptionData.userId)
      .flatMap(tData => tData.groupedLemmas )
      .groupBy{ case (s, _) => s}.toList
      .map{ case (s, group) => (s, group.map(_._2).sum)}

    val novelWords = transcriptionData.groupedLemmas
      .sortBy(_._2)(Ordering[Int].reverse)
      .filter{case (s, c) => !otherLemmas.map(_._1).contains(s) && c >= Settings.MIN_NOVEL_WORD_COUNT}

    val signatureWords = transcriptionData.groupedLemmas
      .sortBy(_._2)(Ordering[Int].reverse)
      .filter{case (s, c) =>
        (c.toDouble / (c + otherLemmas.find{_._1 == s}.map(_._2).getOrElse(0))) >= Settings.SIGNATURE_WORD_MIN_RATIO &&
        c >= Settings.MIN_NOVEL_WORD_COUNT
      }

    ContextualTranscriptionAnalysis(
      transcriptionData.userId,
      novelWords.headOption.getOrElse(("N/A", 0)),
      signatureWords.headOption.getOrElse(("N/A", 0)),
      novelWords.length,
      novelWords.length / transcriptionData.words.length.toDouble
    )
  }

  private def getTokens(document: Document): List[Token] = {
    document.sentences().asScala.flatMap(s => s.tokens().asScala).toList
  }
}

class TranscriptionAnalyzer {}

case class TranscriptionData (
  userId: String,
  words: List[String],
  groupedLemmas: List[(String, Int)]
)

case class StandaloneTranscriptionAnalysis(
  userId: String,
  totalWords: Int,
  vocabNovelty: Double,
  vocabNoveltyNoStop: Double,
  swearPercentage: Double,
  avgWordLength: Double
)

case class ContextualTranscriptionAnalysis(
  userId: String,
  favoriteNovelWordTuple: (String, Int),
  signatureWordTuple: (String, Int),
  novelWordsGreaterThanOne: Int,
  novelWordRatio: Double
)