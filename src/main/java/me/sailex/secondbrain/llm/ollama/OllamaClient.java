package me.sailex.secondbrain.llm.ollama;

import io.github.ollama4j.OllamaAPI;
import io.github.ollama4j.models.chat.*;
import me.sailex.secondbrain.exception.LLMServiceException;

import java.util.List;
import java.util.stream.Collectors;

import lombok.Setter;
import me.sailex.secondbrain.history.Message;
import me.sailex.secondbrain.history.MessageConverter;
import me.sailex.secondbrain.llm.LLMClient;
import me.sailex.secondbrain.util.LogUtil;

public class OllamaClient implements LLMClient {

	@Setter
	private OllamaAPI ollamaAPI;
	private final String model;
	private final String url;

	public OllamaClient(
        String model,
		String url,
		int timeout,
		boolean verbose
	) {
		this.url = url;
		this.ollamaAPI = new OllamaAPI(url);
		this.model = model;
		ollamaAPI.setVerbose(verbose);
		ollamaAPI.setMaxChatToolCallRetries(1);
		ollamaAPI.setRequestTimeoutSeconds(timeout);
//		initModels(defaultPrompt);
	}

	/**
	 * Check if the service is reachable.
	 * @throws LLMServiceException if server is not reachable
	 */
	@Override
	public void checkServiceIsReachable() {
		try {
			boolean isOllamaServerReachable = ollamaAPI.ping();
			if (!isOllamaServerReachable) {
                throw new LLMServiceException("Ollama server is not reachable at: " +  url);
            }
		} catch (Exception e) {
			throw new LLMServiceException("Ollama server is not reachable at: " +  url, e);
		}
	}

	/**
	 * Sends the provided history and functions to Ollama API.
	 * Executes functions called by the LLM.
	 */
	@Override
	public Message chat(List<Message> messages) {
		try {
            List<OllamaChatMessage> chatMessages = messages.stream()
                    .map(MessageConverter::toOllamaChatMessage)
                    .collect(Collectors.toList());
            OllamaChatResult chatResult;
            if (shouldForceCommandJson(messages)) {
                OllamaChatRequest request = OllamaChatRequestBuilder.getInstance(model)
                        .withMessages(chatMessages)
                        .withGetJsonResponse()
                        .build();
                chatResult = ollamaAPI.chat(request);
            } else {
                chatResult = ollamaAPI.chat(model, chatMessages);
            }
			List<OllamaChatMessage> response = chatResult.getChatHistory();
            return MessageConverter.toMessage(response.get(response.size() - 1));
		} catch (Exception e) {
            throw new LLMServiceException("Could not generate Response for last prompt: " + messages.get(messages.size() - 1).getMessage(), e);
		}
	}

    private static boolean shouldForceCommandJson(List<Message> messages) {
        for (Message message : messages) {
            if (message == null || message.getMessage() == null) {
                continue;
            }
            String role = message.getRole() == null ? "" : message.getRole().trim().toLowerCase();
            if (!"system".equals(role)) {
                continue;
            }
            String content = message.getMessage();
            boolean containsOutputShape = content.contains("\"command\"") && content.contains("\"message\"");
            boolean containsCommandInstructions = content.contains("VALID COMMANDS")
                    || content.contains("Respond ONLY with a single valid JSON object")
                    || content.contains("FINAL REMINDER: Output ONLY the JSON object");
            if (containsOutputShape && containsCommandInstructions) {
                return true;
            }
        }
        return false;
    }

//    private void initModels(String defaultPrompt) {
//        pullRequiredModels();
//        createModelWithPrompt(defaultPrompt);
//    }
//
//    private void pullRequiredModels() {
//        try {
//            Set<String> modelNames = ollamaAPI.listModels().stream()
//                    .map(Model::getModelName).collect(Collectors.toSet());
//            boolean requiredModelsExist = modelNames.containsAll(REQUIRED_MODELS);
//            if (!requiredModelsExist) {
//                for (String requiredModel : REQUIRED_MODELS) {
//                    LogUtil.debugInChat("Pulling model: " + requiredModel);
//                    ollamaAPI.pullModel(requiredModel);
//                }
//            }
//        } catch (Exception e) {
//            throw new LLMServiceException("Could not required models: " + REQUIRED_MODELS,  e);
//        }
//    }
//
//    private void createModelWithPrompt(String defaultPrompt) {
//        try {
//            LogUtil.debugInChat("Init model: " + model);
//            ollamaAPI.createModel(CustomModelRequest.builder()
//                    .from(LLAMA_MODEL_NAME)
//                    .model(model)
//                    .system(defaultPrompt)
//                    .license("MIT")
//                    .build());
//        } catch (Exception e) {
//            throw new LLMServiceException("Could not create model: " + model, e);
//        }
//    }
//
//    /**
//     * Removes current model.
//     */
//    public void removeModel() {
//        try {
//            LogUtil.debugInChat("Removing model: " + model);
//            ollamaAPI.deleteModel(model, true);
//        } catch (Exception e) {
//            Thread.currentThread().interrupt();
//            throw new LLMServiceException("Could not remove model: " + model, e);
//        }
//    }

	@Override
	public void stopService() {
        try {
            //removeModel();
        } catch (Exception e) {
            LogUtil.error("Could not delete model: " + e.getMessage());
        }
		//this.service.shutdown();
	}
}
