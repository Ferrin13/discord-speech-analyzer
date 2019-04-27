package speechrecognizer

import java.io.{ByteArrayInputStream, File, SequenceInputStream}
import java.util.Collections

import javax.sound.sampled._
import net.dv8tion.jda.core.audio.AudioReceiveHandler

import collection.JavaConverters._

object AudioFileWriter {
  val MAX_FILE_SIZE_FOR_COMBINE: Long = 1024 * 1000 //Prevent combine from picking up previous combine files
  val AUDIO_FILE_EXTENSION = "wav"

  val audioFormat = new AudioFormat(16000f, 16, 1, true, false)

  def writeWavFile(userId: String, bytesArray: Array[Byte], channelName: String, recordingId: String): Unit = {
    val startTime = System.currentTimeMillis()

    val is = AudioSystem.getAudioInputStream(audioFormat, new AudioInputStream(new ByteArrayInputStream(bytesArray), AudioReceiveHandler.OUTPUT_FORMAT, bytesArray.length))
    val fileName = s"${userId}_audio_${System.currentTimeMillis}"
    val dirPath = Utils.recordingDirectoryPath(channelName, userId, recordingId.toString)
    Utils.createIfNotExists(dirPath)
    AudioSystem.write(is, AudioFileFormat.Type.WAVE, new File( s"$dirPath${File.separator}$fileName.wav"))

    val endTime = System.currentTimeMillis()
    println(s"Wrote file in ${endTime-startTime} ms")
  }

  //Test purposes only
  def combineAllUserRecordings(channelName: String, userId: String): Either[String, Long]  = {
    val lastRecordingId = new File(Utils.userDirectoryPath(channelName, userId)).listFiles()
      .filter(f => f.isDirectory && f.getName != Utils.COMBINE_FILE_DIRECTORY )
      .map(_.getName)

    lastRecordingId.map(combineDirectoryFiles(channelName, userId, _)).toList.foreach {
      case Right(duration) => println(s"Combined in $duration")
      case Left(message) => println(s"Skipped because $message")
    }
    Left("Manual override")
  }

  def combineUserLastRecording(channelName: String, userId: String): Either[String, Long]  = {
    val lastRecordingId = new File(Utils.userDirectoryPath(channelName, userId)).listFiles()
      .filter(f => f.isDirectory && f.getName != Utils.COMBINE_FILE_DIRECTORY )
      .map(_.getName)
      .sorted(Ordering[String].reverse)
      .headOption.getOrElse("NO LAST DIRECTORY")
     combineDirectoryFiles(channelName, userId, lastRecordingId)
  }

  def combineDirectoryFiles(channelName: String, userId: String,  recordingId: String): Either[String, Long] = {
    val startTime = System.currentTimeMillis()
    val files = new File(Utils.recordingDirectoryPath(channelName, userId, recordingId)).listFiles()
      .filter(_.length <= MAX_FILE_SIZE_FOR_COMBINE)
      .filter(_.getName.takeRight(3).toLowerCase() == AUDIO_FILE_EXTENSION)

    if(files.nonEmpty) {
      val combinePath = Utils.userCombinePath(channelName, userId)
      val combineFile = new File(s"$combinePath${File.separator}${recordingId}_combine.wav")
      if (combineFile.exists()) {
        Left("Combine file already exists")
      } else {
        val inputStreams = files.map(AudioSystem.getAudioInputStream)
        val appendedStream = new AudioInputStream(
          new SequenceInputStream(Collections.enumeration(inputStreams.toList.asJava)),
          audioFormat,
          inputStreams.map(_.getFrameLength).sum
        )
        Utils.createIfNotExists(combinePath)
        AudioSystem.write(appendedStream,
          AudioFileFormat.Type.WAVE,
          combineFile
        )
        val duration = System.currentTimeMillis() - startTime
        println(s"Combined files in $channelName for $userId in $duration ms")
        Right(duration)
      }
    } else {
      Left("No recording files found for user")
    }
  }

//  def userDirectoryPath(channelName: String, userId:String): String =
//    s"$FILE_LOCATION_BASE${File.separator}$channelName${File.separator}$userId"
//  def userCombinePath(channelName: String, userId:String): String =
//    s"${userDirectoryPath(channelName, userId)}${File.separator}$COMBINE_FILE_DIRECTORY"
//  def recordingDirectoryPath(channelName: String, userId: String, recordingId: String) =
//    s"${userDirectoryPath(channelName, userId)}${File.separator}$recordingId"
//
//  def getUserIdsInChannelFolder(channelName: String): List[String] = {
//    new File(s"$FILE_LOCATION_BASE${File.separator}$channelName").listFiles().map(_.getName).toList
//  }
//
//  def getChannelDirectoryNames: List[String] = new File(FILE_LOCATION_BASE).listFiles().map(_.getName).toList
//
//  def createIfNotExists(path: String): Boolean = {
//    val directory = new File(path)
//    if(!directory.exists()) {
//      directory.mkdirs()
//      return true
//    }
//    false
//  }
}