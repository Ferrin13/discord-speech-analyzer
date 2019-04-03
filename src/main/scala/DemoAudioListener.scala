import net.dv8tion.jda.core.audio.{AudioReceiveHandler, CombinedAudio, Decoder, UserAudio}
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream

class DemoAudioListener extends AudioReceiveHandler {
  val DEFAULT_VOLUME = 1.0
  val BYTES_PER_SECOND = 192000
  val BYTES_PER_PACKET = 3840

  val secondOfAudio = new Array[Byte](BYTES_PER_SECOND)
  var currentPacketIndex = 0
  var fileWritten = false

  override def canReceiveCombined: Boolean = false

  override def canReceiveUser: Boolean = true

  override def handleCombinedAudio(combinedAudio: CombinedAudio): Unit = {
    val x = combinedAudio.getAudioData(DEFAULT_VOLUME)
    if(!combinedAudio.getUsers.isEmpty) {
      println(s"Received audio from ${combinedAudio.getUsers.toString}")
    }
//    println("Handling Audio")
//    println(s"Audio size ${x.size}")
//    println(s"First user speaking: ${if(!combinedAudio.getUsers.isEmpty){combinedAudio.getUsers.get(0).getName}}")
  }

  override def handleUserAudio(userAudio: UserAudio): Unit = {
    val pcmPacket = userAudio.getAudioData(DEFAULT_VOLUME)
    println(s"Handling audio for ${userAudio.getUser.getName}")

    if(currentPacketIndex < 50) {
      System.arraycopy(pcmPacket, 0, secondOfAudio, currentPacketIndex * BYTES_PER_PACKET, BYTES_PER_PACKET)
      currentPacketIndex += 1
    } else if(!fileWritten) {
      println("Writing file")
      val target = new AudioFormat(16000f, 16, 1, true, false)
      val is = AudioSystem.getAudioInputStream(target, new AudioInputStream(new ByteArrayInputStream(secondOfAudio), AudioReceiveHandler.OUTPUT_FORMAT, BYTES_PER_SECOND))
      AudioSystem.write(is, AudioFileFormat.Type.WAVE, new File( "test_audio.wav"))
//      return IoUtils.toByteArray(new FileInputStream(new File(cache + File.separator + "audio.wav")))
      fileWritten = true
    }


  }
}
