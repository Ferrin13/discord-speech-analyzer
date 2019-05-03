package speechrecognizer

import net.dv8tion.jda.core.events.guild.voice.{GuildVoiceJoinEvent, GuildVoiceUpdateEvent}
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter

import scala.collection.JavaConverters._

class ServerListener extends ListenerAdapter {

  override def onMessageReceived(event: MessageReceivedEvent) {
    val guild = event.getGuild
    val voiceChannels = guild.getVoiceChannels.asScala
    val authorizedVoiceChannel = voiceChannels.find(_.getMembers.asScala.map(_.getUser.getId).contains(Settings.AUTHORIZED_USER_ID))
    val audioManager = guild.getAudioManager

    println(s"Message from ${event.getAuthor.getName}: ${event.getMessage.getContentDisplay}")
    println(s"Voice Channel: ${authorizedVoiceChannel.map(_.getName)}")

    if(event.getAuthor.getId == Settings.AUTHORIZED_USER_ID) {
      getCommand(event.getMessage.getContentRaw) match {
        case ("!start", _) =>
          ServerListener.recordingId = System.currentTimeMillis().toString
          audioManager.setSendingHandler(new SilenceAudioSendHandler()) //Solves Discord Bug
          audioManager.setReceivingHandler(new AudioRecorder())
          if(authorizedVoiceChannel.isEmpty) {
            event.getChannel.sendMessage("Target userId not in a voice channel").queue()
          } else {
            event.getChannel.sendMessage("Starting audio recording").queue()
            ServerListener.currentChanelName = authorizedVoiceChannel.get.getName
            audioManager.openAudioConnection(authorizedVoiceChannel.get)
          }

        case ("!stop", _) =>
          val currentVoiceChannel = audioManager.getConnectedChannel
          audioManager.closeAudioConnection()
          event.getChannel.sendMessage("Stopping audio recording").queue()
          Commands.combine(List(currentVoiceChannel.getName), voiceChannels, event.getChannel, ServerListener.currentChanelName)
          Commands.transcribe(List(currentVoiceChannel.getName), voiceChannels, event.getChannel, ServerListener.currentChanelName)

        case ("!combine", args) =>
          Commands.combine(args, voiceChannels, event.getChannel, ServerListener.currentChanelName)

        case ("!transcribe", args) =>
          Commands.transcribe(args, voiceChannels, event.getChannel, ServerListener.currentChanelName)

        case ("!analyze", args) =>
          Commands.analyze(args, voiceChannels, event.getChannel, ServerListener.currentChanelName)

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

  def getCommand(commandText: String): (String, List[String]) = {
    val tokens = commandText.split(' ')
    (tokens(0), tokens.slice(1, tokens.length).toList)
  }
}


object ServerListener {
//  val FERRIN_USER_ID = "123203630193967104"
//  val SLEEPY_USER_ID = "222210116655513610"
//  val PERKIE_USER_ID = "234089583829057537"
//  val NOBLE_USER_ID = "276407390394515456"
//  val STORM_USER_ID = "302528107603165185"
//  val PARS_USER_ID = "315533205472018442"
//  val APPENDIX_USER_ID = "343917884881371136"
//  val FIIRCE_USER_ID = "358670104424087554"
//  val RR_USER_ID = "366695216511713281"
//  val SMYTH_USER_ID = "398205325330743296"
//  val FIDEL_USER_ID = "437662514646482975"

//  val userIds: Set[String] = Set(FERRIN_USER_ID, SLEEPY_USER_ID, PERKIE_USER_ID, NOBLE_USER_ID, STORM_USER_ID,
//    PARS_USER_ID, APPENDIX_USER_ID, FIIRCE_USER_ID, RR_USER_ID, SMYTH_USER_ID, FIDEL_USER_ID)

//  val userIdNameMap: ImmutableBiMap[String, String] = new ImmutableBiMap.Builder[String, String]()
//      .put(FERRIN_USER_ID, "Ferrin")
//      .put(SLEEPY_USER_ID, "Sleepy")
//      .put(PERKIE_USER_ID, "Perkie")
//      .put(NOBLE_USER_ID, "Noble")
//      .put(STORM_USER_ID, "Storm")
//      .put(PARS_USER_ID, "Pars")
//      .put(APPENDIX_USER_ID, "Appendix")
//      .put(FIIRCE_USER_ID, "Fiirce")
//      .put(RR_USER_ID, "RandomRedneck")
//      .put(SMYTH_USER_ID, "Smyth")
//      .put(FIDEL_USER_ID, "Fidel")
//      .build()

  var recordingId: String = "-1"
  var currentChanelName: String = "CS436Testing"
}
