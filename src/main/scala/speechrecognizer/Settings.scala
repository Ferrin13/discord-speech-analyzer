package speechrecognizer

import java.util

import com.google.common.collect.ImmutableBiMap
import com.typesafe.config.{Config, ConfigFactory}

import scala.collection.JavaConverters._



object Settings {
  val config: Config = ConfigFactory.load()

  val BOT_TOKEN : String = config.getString("bot-token")
  val MIN_WORDS_FOR_ANALYSIS: Int = config.getInt("min-words-for-analysis")
  val AUTHORIZED_USER_ID: String = config.getString("authorized-user-id")
  val USER_ID_NICKNAME_MAP: ImmutableBiMap[String, String] = new ImmutableBiMap.Builder[String, String]()
    .putAll(
      config.getList("user-id-nickname-map").unwrapped().asScala.flatMap{ configItem =>
        configItem.asInstanceOf[util.HashMap[String, String]].asScala
      }.toMap.asJava
    )
    .build()
  val MIN_NOVEL_WORD_COUNT: Int = config.getInt("min-novel-word-count")
  val SIGNATURE_WORD_MIN_RATIO: Double = config.getDouble("signature-word-min-ratio")
  val AUDIO_FILE_LOCATION_BASE: String = config.getString("audio-file-location-base")
  val AUDIO_COMBINE_FILE_DIRECTORY: String = config.getString("audio-combine-file-directory")
}




