package speechrecognizer

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.{AudioTrack, AudioTrackEndReason}

class DemoTrackScheduler(player: AudioPlayer) extends AudioEventAdapter {

  import com.sedmelluq.discord.lavaplayer.player.AudioPlayer

  def queue(track: AudioTrack) = {
    player.playTrack(track)
  }

  override def onPlayerPause(player: AudioPlayer): Unit = {
    // Player was paused
  }

  override def onPlayerResume(player: AudioPlayer): Unit = {
    // Player was resumed
  }

  override def onTrackEnd(player: AudioPlayer, track: AudioTrack, endReason: AudioTrackEndReason): Unit = {
    if (endReason.mayStartNext) {
      // Start next track
    }
    // endReason == FINISHED: A track finished or died by an exception (mayStartNext = true).
    // endReason == LOAD_FAILED: Loading of a track failed (mayStartNext = true).
    // endReason == STOPPED: The player was stopped.
    // endReason == REPLACED: Another track started playing while this had not finished
    // endReason == CLEANUP: Player hasn't been queried for a while, if you want you can put a
    //                       clone of this back to your queue
  }

  override def onTrackException(player: AudioPlayer, track: AudioTrack, exception: FriendlyException): Unit = {
    // An already playing track threw an exception (track end event will still be received separately)
  }

  override def onTrackStuck(player: AudioPlayer, track: AudioTrack, thresholdMs: Long): Unit = {
    // Audio track has been unable to provide us any audio, might want to just start a new track
  }

}
