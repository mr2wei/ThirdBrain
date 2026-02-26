package me.sailex.secondbrain.event

import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import me.sailex.altoclef.AltoClefController
import me.sailex.secondbrain.config.NPCConfig
import me.sailex.secondbrain.constant.Instructions
import me.sailex.secondbrain.context.ContextProvider
import me.sailex.secondbrain.history.ConversationHistory
import me.sailex.secondbrain.history.Message
import me.sailex.secondbrain.llm.LLMClient
import me.sailex.secondbrain.llm.openai.OpenAiClient
import me.sailex.secondbrain.llm.player2.Player2APIClient
import me.sailex.secondbrain.llm.roles.Player2ChatRole
import me.sailex.secondbrain.util.LogUtil
import me.sailex.secondbrain.util.PromptFormatter
import net.minecraft.util.math.BlockPos
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class NPCEventHandler(
    private val llmClient: LLMClient,
    private val history: ConversationHistory,
    private val contextProvider: ContextProvider,
    private val controller: AltoClefController,
    private val config: NPCConfig,
): EventHandler {
    companion object {
        private val gson = GsonBuilder()
            .setLenient()
            .create()
        private val BLOCKED_COMMAND_KEYWORDS = listOf("attack", "kill", "break", "place", "mine", "punch", "destroy")
    }

    private val executorService: ThreadPoolExecutor = ThreadPoolExecutor(
        1, 1, 0L, TimeUnit.MILLISECONDS,
        ArrayBlockingQueue(10)
    )

    /**
     * Processes an event asynchronously by allowing call actions from llm using the specified prompt.
     * Saves the prompt and responses in conversation history.
     *
     * @param prompt prompt of a user or system e.g. chatmessage of a player
     */
    override fun onEvent(prompt: String) {
        try {
            executorService.execute task@{
                try {
                    LogUtil.info("onEvent: $prompt")

                    val worldContext = contextProvider.buildContext()
                    val zoneAwarePrompt = applyZoneSpecificBehaviour(prompt, worldContext.state().position())
                    val formattedPrompt: String = PromptFormatter.format(zoneAwarePrompt, worldContext)

                    history.add(Message(formattedPrompt, Player2ChatRole.USER.toString().lowercase()))
                    val systemPrompt = Instructions.getLlmSystemPrompt(
                        config.npcName,
                        config.getEffectiveLlmCharacter(),
                        controller.commandExecutor.allCommands(),
                        config.llmType
                    )
                    val response = llmClient.chat(history.buildMessagesForApi(systemPrompt))
                    history.add(response)

                    val parsedMessage = parse(response.message)
                    execute(parsedMessage.command)

                    //prevent printing multiple times the same when llm is running in command syntax errors
                    if (parsedMessage.message != history.getLastMessage()) {
                        // Always send text chat; TTS is additional output when enabled.
                        controller.controllerExtras.chat(parsedMessage.message)
                        if (!config.isTTS) return@task

                        when (llmClient) {
                            is Player2APIClient -> llmClient.startTextToSpeech(parsedMessage.message)
                            is OpenAiClient -> {
                                try {
                                    llmClient.startTextToSpeech(parsedMessage.message)
                                } catch (e: Exception) {
                                    LogUtil.error("OpenAI TTS failed", e)
                                }
                            }
                            else -> {}
                        }
                    }
                } catch (e: Throwable) {
                    LogUtil.debugInChat("Could not generate a response: " + buildErrorMessage(e))
                    LogUtil.error("Error occurred handling event: $prompt", e)
                }
            }
        } catch (e: RejectedExecutionException) {
            LogUtil.error("Dropped NPC event because the queue is full (size=${executorService.queue.size}): $prompt", e)
        }
    }

    override fun stopService() {
        executorService.shutdownNow()
    }

    override fun queueIsEmpty(): Boolean {
        return executorService.queue.isEmpty()
    }

    //TODO: refactor this into own class
    private fun parse(content: String): CommandMessage {
        return try {
            parseContent(content)
        } catch (_: JsonParseException) {
            val cleanedContent = content
                .replace("```json", "")
                .replace("```", "")
            try {
                parseContent(cleanedContent)
            } catch (e: JsonParseException) {
                throw CustomEventException("The selected model may be too small to understand the context or to reliably produce valid JSON. " +
                        "Please switch to a larger or more capable LLM model.", e)
            }
        }
    }

    private fun parseContent(content: String): CommandMessage {
        return gson.fromJson(content, CommandMessage::class.java)
    }

    fun execute(command: String) {
        val cmdExecutor = controller.commandExecutor
        val normalized = command.lowercase()
        val safeCommand = if (BLOCKED_COMMAND_KEYWORDS.any { normalized.contains(it) }) "idle" else command
        val commandWithPrefix = if (cmdExecutor.isClientCommand(safeCommand)) {
            safeCommand
        } else {
            cmdExecutor.commandPrefix + safeCommand
        }
        cmdExecutor.execute(commandWithPrefix, {
//            if (queueIsEmpty()) {
//                //this.onEvent(Instructions.COMMAND_FINISHED_PROMPT.format(commandWithPrefix))
//            }
        }, {
            this.onEvent(Instructions.COMMAND_ERROR_PROMPT.format(commandWithPrefix, it.message))
            LogUtil.error("Error executing command: $commandWithPrefix", it)
        })
    }

    data class CommandMessage(
        val command: String,
        val message: String
    )

    private fun buildErrorMessage(exception: Throwable): String {
        val chain = generateSequence(exception) { it.cause }.toList()
        val custom = chain.filterIsInstance<CustomEventException>().firstOrNull()
        if (custom != null) return custom.message ?: "Custom event error"
        val meaningful = chain.firstOrNull { !it.message.isNullOrBlank() && it.message!!.trim() != "ERROR :" }
        if (meaningful != null) return meaningful.message!!
        return "LLM request failed: provider returned a non-2xx response with an empty error body. Check base URL, model, and API key."
    }

    private fun applyZoneSpecificBehaviour(prompt: String, position: BlockPos): String {
        val matchingZone = config.zoneBehaviors
            .filter { it.instructions.isNotBlank() && it.contains(position) }
            .maxByOrNull { it.priority }
            ?: return prompt

        return """
            $prompt

            Additional zone instructions (zone: ${matchingZone.name}, priority: ${matchingZone.priority}):
            ${matchingZone.instructions}
        """.trimIndent()
    }

}
