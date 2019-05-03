package speechrecognizer

import net.dv8tion.jda.core.entities.{MessageChannel, VoiceChannel}

import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContextExecutor, Future}

object Commands {
  implicit val executor: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global
  val DIVIDER_STRING = "------------------------------------------------------------------"

  def combine(args: List[String],
              voiceChannels: mutable.Buffer[VoiceChannel],
              messageChanel: MessageChannel,
              defaultChannelName: String
  ): Unit = {
    val channels = getChannelsFromArgs(args, voiceChannels, defaultChannelName)
    val message = channels.map { channels =>
      messageChanel.sendMessage(s"Combining last audio recordings in ${channels.map(_.getName).mkString(", ")}\n").queue()
      channels.map{ channel =>
        val userDurations = Utils.getUserIdsInChannelFolder(channel.getName).map { userId =>
          (userId, AudioFileWriter.combineUserLastRecording(channel.getName, userId))
        }
        val messageBody = userDurations.map{ ud =>
          val userName = Utils.userIdToName(ud._1).getOrElse(ud._1)
          ud._2 match {
            case Right(duration) => s"\tCombined $userName's audio in $duration milliseconds"
            case Left(s) => s"\tSkipping $userName because ${s.toLowerCase}"
          }
        }.mkString("\n")

        val totalDuration = userDurations.map(_._2).map {
          case Right(duration) => duration
          case _ => 0
        }.sum

        s"Combined user audio for ${channel.getName} in $totalDuration ms: \n$messageBody"
      }.reduce((s1, s2) => s"$s1\n\n$s2")
    }
    messageChanel.sendMessage(message.getOrElse("Error finding channels")).queue()
  }

  def transcribe(args: List[String],
              voiceChannels: mutable.Buffer[VoiceChannel],
              messageChanel: MessageChannel,
              defaultChannelName: String
  ): Unit = {
    val channelsOption = getChannelsFromArgs(args, voiceChannels, defaultChannelName)
    val messageFutureOption = channelsOption.map { channels =>
      messageChanel.sendMessage(s"Transcribing last audio recordings in ${channels.map(_.getName).mkString(", ")}\n").queue()
      Future.sequence( channels.map { channel =>
        val startTime = System.currentTimeMillis()
        val userDurations = Utils.getUserIdsInChannelFolder(channel.getName).map { userId =>
          (userId, VoiceRecognizer.transcribeUserLastRecording(channel.getName, userId))
        }
        val messageBodyFuture = Future.sequence(userDurations.map{ ud =>
          val userName = Utils.userIdToName(ud._1).getOrElse(ud._1)
          ud._2.map {
            case Right(duration) =>
              val userMessage = s"Uploaded and transcribed $userName's audio in ${Utils.msAsSeconds(duration)} "
              messageChanel.sendMessage(userMessage).queue()
              "\t" + userMessage
            case Left(s) => s"\tSkipping $userName because ${s.toLowerCase}"
          }
        }).map(_.mkString("\n"))

        messageBodyFuture.map{ messageBody =>
          val totalDuration = System.currentTimeMillis() - startTime
          s"Transcribed user audio for ${channel.getName} in ${Utils.msAsSeconds(totalDuration)}: \n$messageBody"
        }
      }).map(_.reduce((s1, s2) => s"$s1\n\n$s2"))
    }
    val message = Await.result(messageFutureOption.getOrElse(Future.successful("Error finding channels")), Duration.Inf)
    messageChanel.sendMessage(message).queue()
  }

  def analyze(args: List[String],
                 voiceChannels: mutable.Buffer[VoiceChannel],
                 messageChanel: MessageChannel,
                 defaultChannelName: String
  ): Unit = {
    val channelOption = getChannelsFromArgs(args, voiceChannels, defaultChannelName).flatMap(_.headOption)
    if(channelOption.isDefined) {
      val channel = channelOption.get
      messageChanel.sendMessage(s"Beginning analysis of ${channel.getName}\n" + DIVIDER_STRING).queue()
      val channelsUserIds = Utils.getUserIdsInChannelFolder(channel.getName)
      val userTranscriptionData = Future.sequence(channelsUserIds.map { userId =>
        TranscriptionAnalyzer.userTranscriptionData(channel.getName, userId)
      })
      val userSummaryFuture = userTranscriptionData.map { tDataList =>
        tDataList.sortBy(tData => Utils.userIdToName(tData.userId)).map { tData =>
          val basicAnalysis = TranscriptionAnalyzer.standaloneTranscriptionAnalysis(tData)
          val contextualAnalysis = TranscriptionAnalyzer.contextualTranscriptionAnalysis(tData, tDataList)
          val text = basicAnalysis.totalWords match {
            case t if t >= Settings.MIN_WORDS_FOR_ANALYSIS => messageFromTranscriptionAnalysis(basicAnalysis, contextualAnalysis)
            case _ => s"Skipped ${Utils.userIdToName(basicAnalysis.userId)} because he/she has less than ${Settings.MIN_WORDS_FOR_ANALYSIS} recorded words\n"
          }
          text + DIVIDER_STRING
        }
      }
      val messages = Await.result(userSummaryFuture, Duration.Inf)
      messages.foreach(m => messageChanel.sendMessage(m).queue())
      messageChanel.sendMessage(s"Finished channel analysis").queue()
    } else {
      messageChanel.sendMessage("Cannot find specified channel").queue()
    }
  }

  private def messageFromTranscriptionAnalysis(
    standaloneAnalysis: StandaloneTranscriptionAnalysis,
    contextualAnalysis: ContextualTranscriptionAnalysis
  ): String = {
    val userName = Utils.userIdToName(standaloneAnalysis.userId).getOrElse(standaloneAnalysis.userId)
    val builder = new mutable.StringBuilder()
    builder.append(s"Summary stats for $userName (${standaloneAnalysis.totalWords} words analyzed):\n")
    builder.append(s"\tFavorite novel word: ${wordTupleToString(contextualAnalysis.favoriteNovelWordTuple)}\n")
    builder.append(s"\tSignature word (at least ${Settings.SIGNATURE_WORD_MIN_RATIO * 100}% of all channel occurrences): ${wordTupleToString(contextualAnalysis.signatureWordTuple)}\n")
    builder.append(s"\tNovel words used (must be used at least twice): ${contextualAnalysis.novelWordsGreaterThanOne}\n")
    builder.append(s"\tNovel word percentage: ${Utils.doubleAsPercentage(contextualAnalysis.novelWordRatio)}\n")
    builder.append(s"\tWord diversity percentage: ${Utils.doubleAsPercentage(standaloneAnalysis.vocabNovelty)}\n")
    builder.append(s"\tWord diversity percentage (No stop words): ${Utils.doubleAsPercentage(standaloneAnalysis.vocabNoveltyNoStop)}\n")
    builder.append(f"\tSwear percentage: ${Utils.doubleAsPercentage(standaloneAnalysis.swearPercentage)}\n")
    builder.append(s"\tAverage word length: ${Utils.formatDouble(standaloneAnalysis.avgWordLength)}\n")
    builder.toString()
  }

  private def wordTupleToString(wordTuple: (String, Int)): String = {
    s""""${wordTuple._1}" (used ${wordTuple._2} times)"""
  }

  private def getChannelsFromArgs(args: List[String],
                                  voiceChannels: mutable.Buffer[VoiceChannel],
                                  defaultChannelName: String
  ): Option[List[VoiceChannel]] = {
    args.find(_.toLowerCase == "all").map{_ =>
      Some(voiceChannels.filter(vc => Utils.getChannelDirectoryNames.contains(vc.getName)).toList)
    }.getOrElse({
      val chanelName = args.find(_.toLowerCase != "all").getOrElse(defaultChannelName)
      voiceChannels.find(_.getName == chanelName).map(List(_))
    })
  }
}
