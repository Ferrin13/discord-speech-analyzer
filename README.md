# Discord Speech Analyzer Bot

A Discord bot written in Scala that records, transcribes, and analyzes user audio in a Discord server. 

# Authors 
Caleb Cook

# Building
The project requires the host to have Google Cloud Storage and Google Speech To Text API configured. 
The three "Before You Begin" steps [here](https://cloud.google.com/speech-to-text/docs/quickstart-client-libraries#client-libraries-install-java) 
are sufficient.

The project needs a Discord Application Token, which requires creating an application [here](https://discordapp.com/developers/applications/).
After creating the application set the configuration value `bot-token` to the bot token. 

In order for the speech analysis to work, `stanford-english-corenlp-models` and `stanford-english-kbp-corenlp-models`
need to be in the java classpath. (These files are large, so they are not included by default in the SBT build)

Set the `authorized-user-id` configuration key to the discord user id of the user that will be issuing commands to the bot. 




