# Discord Speech Analyzer Bot

A Discord bot written in Scala that records, transcribes, and analyzes user audio in a Discord server. 

# Authors 
Caleb Cook

# Building
The following pre-requisites are required before building the project:

1. Properly configured Google Cloud Storage and Google Speech To Text API, the three "Before You Begin" steps
[here](https://cloud.google.com/speech-to-text/docs/quickstart-client-libraries#client-libraries-install-java) 
are sufficient.

2. A Discord Application Token, which requires creating an application
 [here](https://discordapp.com/developers/applications/).
After creating the application set the configuration value `bot-token` to the bot token. 

3. `stanford-english-corenlp-models` and `stanford-english-kbp-corenlp-models` jars need to be in the java classpath.
 (These files are large, so they are not included by default in the SBT build)

The following configuration keys need to be set (along with `bot-token`):

* `authorized-user-id` -- Discord User Id of the user that will be issuing commands to the bot
* `google-storage-bucket-name` -- Name of the Google Storage Bucket where audio files will be stored

Once these values are set, the project can be run simply with `sbt run`. The recommended vm arguments are 
`-Xms512M -Xmx3000M -Xss1M -XX:+CMSClassUnloadingEnabled`, the larger heap size is particularly important, as the speech
analysis can potentially require large amounts of memory. 







