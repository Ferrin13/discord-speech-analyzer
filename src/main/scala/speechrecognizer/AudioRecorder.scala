package speechrecognizer

import net.dv8tion.jda.core.audio.{AudioReceiveHandler, CombinedAudio, UserAudio}
import net.dv8tion.jda.core.entities.User

import scala.collection.mutable

class AudioRecorder extends AudioReceiveHandler {
  val DEFAULT_VOLUME = 1.0
  val BYTES_PER_SECOND = 192000
  val BYTES_PER_PACKET = 3840
  val SECONDS_PER_SECTION = 10
  val PACKETS_PER_SECOND = 50
  val USER_INACTIVITY_TIMEOUT_MS = 5000

  var fileWritten = false
  var userLastTimeRecorded: mutable.HashMap[String, Long] = mutable.HashMap()
  val userPacketIndexMap: mutable.HashMap[String, Int]  = new mutable.HashMap()
  val userAudioSectionMap: mutable.HashMap[String, Array[Byte]] = new mutable.HashMap()

  override def canReceiveCombined: Boolean = false

  override def canReceiveUser: Boolean = true

  override def handleCombinedAudio(combinedAudio: CombinedAudio): Unit = ???

  override def handleUserAudio(userAudio: UserAudio): Unit = {
    val pcmPacket = userAudio.getAudioData(DEFAULT_VOLUME)
    handleUser(userAudio.getUser)
    val userId = userAudio.getUser.getId
    val currentPacketIndex = userPacketIndexMap.getOrElseUpdate(userId, 0)
    val audioSection = userAudioSectionMap.getOrElseUpdate(userId, new Array[Byte](BYTES_PER_SECOND * SECONDS_PER_SECTION))

    System.arraycopy(pcmPacket, 0, audioSection, currentPacketIndex * BYTES_PER_PACKET, BYTES_PER_PACKET)
    userPacketIndexMap.update(userId, currentPacketIndex + 1)

    if(currentPacketIndex + 1 == PACKETS_PER_SECOND * SECONDS_PER_SECTION) {
      println("Writing file")
      AudioFileWriter.writeWavFile(userAudio.getUser.getId, audioSection, ServerListener.currentChanelName, ServerListener.recordingId)
      userPacketIndexMap.update(userId, 0)
    }
  }

  private def handleUser(user: User): Unit = {
    val currentMs = System.currentTimeMillis
    val lastRecordedTime = userLastTimeRecorded.getOrElse(user.getId, 0.toLong)
    userLastTimeRecorded += ((user.getId, currentMs))
    if(currentMs - lastRecordedTime > USER_INACTIVITY_TIMEOUT_MS) {
      println(s"Recording ${user.getName}")
    }
  }
}
