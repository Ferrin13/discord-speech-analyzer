package speechrecognizer

import java.io.File
import java.util.concurrent.Executor

import com.google.api.core.ApiFuture

import scala.concurrent.{Future, Promise}
import scala.util.Try

object Utils {
  private val FILE_LOCATION_BASE = Settings.AUDIO_FILE_LOCATION_BASE
  private val COMBINE_FILE_DIRECTORY = Settings.AUDIO_COMBINE_FILE_DIRECTORY

  implicit class ListenableFutureDecorator[T](val f: ApiFuture[T]) extends AnyVal {
    def asScala(implicit e: Executor): Future[T] = {
      val p = Promise[T]()
      f.addListener(() => p.complete(Try(f.get())), implicitly[Executor])
      p.future
    }
  }

  def userDirectoryPath(channelName: String, userId:String): String =
    s"$FILE_LOCATION_BASE${File.separator}$channelName${File.separator}$userId"
  def userCombinePath(channelName: String, userId:String): String =
    s"${userDirectoryPath(channelName, userId)}${File.separator}$COMBINE_FILE_DIRECTORY"
  def recordingDirectoryPath(channelName: String, userId: String, recordingId: String) =
    s"${userDirectoryPath(channelName, userId)}${File.separator}$recordingId"

  def getUserIdsInChannelFolder(channelName: String): List[String] = {
    new File(s"$FILE_LOCATION_BASE${File.separator}$channelName").listFiles().map(_.getName).toList
  }

  def getChannelDirectoryNames: List[String] = new File(FILE_LOCATION_BASE).listFiles().map(_.getName).toList

  def createIfNotExists(path: String): Boolean = {
    val directory = new File(path)
    if(!directory.exists()) {
      directory.mkdirs()
      return true
    }
    false
  }

  def userIdToName(userId: String): Option[String] = {
    Option(Settings.USER_ID_NICKNAME_MAP.get(userId))
  }

  def msAsSeconds(milliseconds: Long): String = {
    s"${milliseconds / 1000.toDouble} seconds"
  }
  def formatDouble(num: Double): String = {
    f"$num%1.3f"
  }
  def doubleAsPercentage(num: Double): String = {
    f"${num * 100}%1.2f" + "%"
  }
}


