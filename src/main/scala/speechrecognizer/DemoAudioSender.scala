package speechrecognizer

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame
import net.dv8tion.jda.core.audio.AudioSendHandler

class DemoAudioSender(audioPlayer: AudioPlayer) extends AudioSendHandler {
  var lastFrame: AudioFrame = _

  override def canProvide: Boolean = {
    lastFrame = audioPlayer.provide()
    lastFrame != null
  }

  override def provide20MsAudio(): Array[Byte] = {
    lastFrame.getData
  }

  override def isOpus: Boolean = true
}
