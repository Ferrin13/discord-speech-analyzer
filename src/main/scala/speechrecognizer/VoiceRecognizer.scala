package speechrecognizer

import java.io.{File, FileInputStream, FileWriter}
import java.nio.file.Path

import com.google.cloud.speech.v1._
import com.google.cloud.storage.{BlobInfo, StorageOptions}
import org.apache.commons.io.IOUtils

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContextExecutor, Future}

class VoiceRecognizer() {}

object VoiceRecognizer {
  implicit val executor: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global

  private final val GC_PREFIX = s"gs://${Settings.GOOGLE_STORAGE_BUCKET_NAME}/"
  private final val DEFAULT_RECOGNITION_CONFIG: RecognitionConfig = RecognitionConfig.newBuilder
    .setSampleRateHertz(16000)
    .setLanguageCode("en-US")
    .build()


  def transcribeUserLastRecording(channelName: String, userId: String): Future[Either[String, Long]] = {
    new File(Utils.userCombinePath(channelName, userId)).listFiles()
      .filter(_.getName.takeRight(3) == AudioFileWriter.AUDIO_FILE_EXTENSION)
      .sortBy(_.getName)(Ordering[String].reverse)
      .headOption.map(f => uploadAndTranscribeFile(Path.of(f.getPath)))
      .getOrElse(Future.successful(Left(s"No combine file found in $channelName for $userId")))
  }

  def uploadAndTranscribeFile(filePath: Path): Future[Either[String, Long]] = {
    val startTime = System.currentTimeMillis()

    val transcriptionFilePath = changeExtension(filePath, "txt").toString
    val metaDataFilePath = changeExtension(filePath, "metadata").toString

    (new File(transcriptionFilePath).exists(), new File(metaDataFilePath).exists()) match {
      case (true, true) => Future.successful(Left("Transcription and metadata file already exist"))
      case (true, false) => Future.successful(Left("Transcription file already exist"))
      case (false, true) => Future.successful(Left("Metadata file already exist"))
      case _ =>
        uploadFile(filePath).flatMap { gcPath =>

          // Performs speech recognition on the audio file
          asyncRecognize(gcPath).map{ response =>
            val (transcription, confidence) = handleResults(response.getResultsList.asScala.toList)

            //TODO Make async
            val transcriptionWriter = new FileWriter(new File(transcriptionFilePath))
            transcriptionWriter.write(transcription)
            transcriptionWriter.close()

            val metadataWriter = new FileWriter(new File(metaDataFilePath))
            metadataWriter.write(s"Confidence: $confidence \n")
            metadataWriter.close()

            Right(System.currentTimeMillis() - startTime)
          }
        }
    }
  }

  private def asyncRecognize(gcPath: String): Future[LongRunningRecognizeResponse] = Future {
    val client: SpeechClient = SpeechClient.create()
    val audio: RecognitionAudio = RecognitionAudio.newBuilder
      .setUri(gcPath)
      .build()

    val response = client.longRunningRecognizeAsync(DEFAULT_RECOGNITION_CONFIG, audio)

    //This while/sleep loop is extremely ugly, but it is what is shown in the Documention Example, the futures do not work by default
    while (!response.isDone) {
      Thread.sleep(5000)
    }
    client.close()
    response.get()
  }

  private def handleResults(results: List[SpeechRecognitionResult]): (String, Float) = {
    val alternatives = results.map(_.getAlternatives(0))
    val resultString = alternatives.map(_.getTranscript.trim).mkString("\n")
    val avgConfidence = results.map(_.getAlternatives(0).getConfidence).sum / alternatives.length
    (resultString, avgConfidence)
  }

  private def uploadFile(filePath: Path): Future[String] = Future {
    val transportOptions = StorageOptions
      .getDefaultHttpTransportOptions
      .toBuilder
      .setConnectTimeout(Settings.AUDIO_FILE_UPLOAD_TIMEOUT_MS)
      .setReadTimeout(Settings.AUDIO_FILE_UPLOAD_TIMEOUT_MS)
      .build

    val storageOptions = StorageOptions.newBuilder.setTransportOptions(transportOptions).build
    val storage = storageOptions.getService
    val bucket = storage.get(Settings.GOOGLE_STORAGE_BUCKET_NAME)

    val fileName = filePath.toString
    val startTime = System.currentTimeMillis()
    val blobInfo = storage.create(
      BlobInfo
        .newBuilder(bucket.getName, fileName)
        .build(),
      IOUtils.toByteArray(new FileInputStream(new File(filePath.toString)))
    )
    s"$GC_PREFIX$fileName"
  }

  //A bit hacky, but Path has no built-in extension altering method
  private def changeExtension(path: Path, newExtension: String): Path = {
    Path.of(path.toString.dropRight(3) + newExtension)
  }
}


