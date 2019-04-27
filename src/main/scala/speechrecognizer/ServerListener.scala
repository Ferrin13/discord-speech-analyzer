package speechrecognizer

import java.nio.file.Path

import com.sedmelluq.discord.lavaplayer.player.{AudioLoadResultHandler, AudioPlayer, DefaultAudioPlayerManager}
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.{AudioPlaylist, AudioTrack}
import net.dv8tion.jda.core.events.guild.voice.{GuildVoiceJoinEvent, GuildVoiceUpdateEvent}
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter

import collection.JavaConverters._

class ServerListener extends ListenerAdapter {
  type FailureString = String //Makes failure strings more clear


  override def onMessageReceived(event: MessageReceivedEvent) {
    val guild = event.getGuild
    val user = event.getAuthor
    val voiceChannels = guild.getVoiceChannels.asScala
    val ferrinVoiceChannel = voiceChannels.find(_.getMembers.asScala.map(_.getUser.getId).contains(ServerListener.FERRIN_USER_ID))
    val audioManager = guild.getAudioManager
    val defaultFilePath = Path.of("C:\\Users\\CalebsLaptop\\IdeaProjects\\CS436FinalProject\\RecordedAudio\\PUBG\\366695216511713281\\1554950555575\\366695216511713281_audio_1554950579669.wav")

    println(s"Message from ${event.getAuthor.getName}: ${event.getMessage.getContentDisplay}")
    println(s"Voice Channel: ${ferrinVoiceChannel.map(_.getName)}")

    if(event.getAuthor.getId == ServerListener.FERRIN_USER_ID) {
      getCommand(event.getMessage.getContentRaw) match {
        case ("!start", _) =>
          ServerListener.recordingId = System.currentTimeMillis().toString
          audioManager.setSendingHandler(new SilenceAudioSendHandler()) //Solves Discord Bug
          audioManager.setReceivingHandler(new AudioRecorder())
          if(ferrinVoiceChannel.isEmpty) {
            event.getChannel.sendMessage("Target userId not in a voice channel").queue()
          } else {
            event.getChannel.sendMessage("Starting audio recording").queue()
            ServerListener.currentChanelName = ferrinVoiceChannel.get.getName
            audioManager.openAudioConnection(ferrinVoiceChannel.get)
          }

        case ("!stop", _) =>
          val currentVoiceChannel = audioManager.getConnectedChannel
          audioManager.closeAudioConnection()
          event.getChannel.sendMessage("Stopping audio recording").queue()
          Commands.combine(List(currentVoiceChannel.getName), voiceChannels, event.getChannel, ServerListener.currentChanelName)
          Commands.transcribe(List(currentVoiceChannel.getName), voiceChannels, event.getChannel, ServerListener.currentChanelName)


        //USAGE: !combine [channel] [all]
        case ("!combine", args) =>
          Commands.combine(args, voiceChannels, event.getChannel, ServerListener.currentChanelName)

        case ("!play", _) =>
          audioManager.setSendingHandler(new DemoAudioSender(getPlayer()))
          if(ferrinVoiceChannel.isEmpty) {
            event.getChannel.sendMessage("Target userId not in a voice channel").queue()
          } else {
            event.getChannel.sendMessage("Playing audio").queue()
            audioManager.openAudioConnection(ferrinVoiceChannel.get)
          }

        case ("!transcribe", args) =>
          Commands.transcribe(args, voiceChannels, event.getChannel, ServerListener.currentChanelName)
//          event.getChannel.sendMessage("Beginning to transcribe audio").queue()
//          val filePath = args.headOption.map(Path.of(_)).getOrElse(defaultFilePath)
//          val message = VoiceRecognizer.uploadAndTranscribeFile(filePath) match {
//            case Right(duration) => s"Uploaded and transcribed file in ${duration / 1000.toDouble} seconds"
//            case Left(reason) => s"Skipped file because $reason"
//          }
//          event.getChannel.sendMessage(message).queue()

        case _ => println("Unknown command")
      }
    }
  }


  override def onGuildVoiceJoin(event: GuildVoiceJoinEvent): Unit = {
    println(s"${event.getMember.getUser.getName} joined: ${event.getChannelJoined.getName}")
  }

  override def onGuildVoiceUpdate(event: GuildVoiceUpdateEvent): Unit = {
//    println(s"Guild Voice Updated: ${event.get}")

  }

  def getPlayer(): AudioPlayer = {
    val playerManager = new DefaultAudioPlayerManager
    AudioSourceManagers.registerRemoteSources(playerManager)
    val player = playerManager.createPlayer()
    val trackScheduler = new DemoTrackScheduler(player)
    playerManager.loadItem("nkqVm5aiC28", new AudioLoadResultHandler() {

      def trackLoaded(track: AudioTrack) {
        trackScheduler.queue(track)
      }

      def playlistLoaded(playlist: AudioPlaylist) {
        playlist.getTracks.forEach(x => trackScheduler.queue(x))
      }

      def noMatches() {
        println("No matches found")
      }

      def loadFailed(throwable: FriendlyException) {
        println(s"Load failed ${throwable.getMessage}")
      }
    })
    player
  }

  def getCommand(commandText: String): (String, List[String]) = {
    val tokens = commandText.split(' ')
    (tokens(0), tokens.slice(1, tokens.length).toList)
  }
}


object ServerListener {
  val FERRIN_USER_ID = "123203630193967104"
  val SLEEPY_USER_ID = "222210116655513610"
  val PERKIE_USER_ID = "234089583829057537"
  val NOBLE_USER_ID = "276407390394515456"
  val STORM_USER_ID = "302528107603165185"
  val PARS_USER_ID = "315533205472018442"
  val APPENDIX_USER_ID = "343917884881371136"
  val FIIRCE_USER_ID = "358670104424087554"
  val RR_USER_ID = "366695216511713281"
  val SMYTH_USER_ID = "398205325330743296"
  val FIDEL_USER_ID = "437662514646482975"

  val userIds: Set[String] = Set(FERRIN_USER_ID, SLEEPY_USER_ID, PERKIE_USER_ID, NOBLE_USER_ID, STORM_USER_ID,
    PARS_USER_ID, APPENDIX_USER_ID, FIIRCE_USER_ID, RR_USER_ID, SMYTH_USER_ID, FIDEL_USER_ID)

  val userNameIdMap: Map[String, String] = Map(
    FERRIN_USER_ID -> "Ferrin",
    SLEEPY_USER_ID -> "Sleepy",
    PERKIE_USER_ID -> "Perkie",
    NOBLE_USER_ID -> "Noble",
    STORM_USER_ID -> "Storm",
    PARS_USER_ID -> "Pars",
    APPENDIX_USER_ID -> "Appendix",
    FIIRCE_USER_ID -> "Fiirce",
    RR_USER_ID -> "RandomRedneck",
    SMYTH_USER_ID -> "Smyth",
    FIDEL_USER_ID -> "Fidel"
  )

  var recordingId: String = "-1"
  var currentChanelName: String = "CS436Testing"
}
