package me.sailex.secondbrain.llm.openai;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.http.HttpResponse;
import com.openai.models.audio.speech.SpeechCreateParams;
import com.openai.models.audio.speech.SpeechModel;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import me.sailex.secondbrain.exception.LLMServiceException;
import me.sailex.secondbrain.history.Message;
import me.sailex.secondbrain.llm.LLMClient;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OpenAiClient implements LLMClient {

	private final OpenAIClient openAiService;
	private final String openAiModel;
    private final String voiceId;
    private final List<SourceDataLine> activeLines = Collections.synchronizedList(new ArrayList<>());

	/**
	 * Constructor for OpenAiClient.
	 *
	 * @param apiKey  the api key
	 */
	public OpenAiClient(String model, String apiKey, String baseUrl, int timeout, String voiceId) {
		this.openAiModel = model;
        this.voiceId = voiceId;
		this.openAiService = OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .baseUrl(normalizeBaseUrl(baseUrl))
                .timeout(Duration.ofSeconds(Math.max(timeout, 1)))
                .build();
	}

	@Override
	public Message chat(List<Message> messages) {
		try {
            ChatCompletionCreateParams.Builder requestBuilder = ChatCompletionCreateParams.builder()
                    .model(openAiModel);
            for (Message message : messages) {
                addMessage(requestBuilder, message);
            }

            ChatCompletion response = openAiService.chat().completions().create(requestBuilder.build());
            String content = response.choices().stream()
                    .findFirst()
                    .flatMap(choice -> choice.message().content())
                    .orElse("");
            return new Message(content, "assistant");
        } catch (Exception e) {
            Throwable root = e;
            while (root.getCause() != null) root = root.getCause();
            String rootMessage = root.getMessage();
            if (rootMessage == null || rootMessage.isBlank() || "ERROR :".equals(rootMessage.trim())) {
                rootMessage = "Provider returned a non-2xx response with an empty error body. Check base URL, model, and API key.";
            }
            throw new LLMServiceException("Could not generate Response for prompt: " + messages.get(messages.size() - 1).getMessage()
                    + "\nRoot cause: " + root.getClass().getSimpleName() + ": " + rootMessage, e);
        }
    }

    @Override
    public void checkServiceIsReachable() {
        //i guess its always reachable?
    }

    @Override
    public void stopService() {
        synchronized (activeLines) {
            for (SourceDataLine line : new ArrayList<>(activeLines)) {
                try {
                    line.stop();
                    line.flush();
                    line.close();
                } catch (Exception ignored) {
                    // best effort audio line cleanup
                }
            }
            activeLines.clear();
        }
	}

    public void startTextToSpeech(String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        try {
            SpeechCreateParams request = SpeechCreateParams.builder()
                    .model(SpeechModel.GPT_4O_MINI_TTS)
                    .voice(resolveVoice(voiceId))
                    .input(message)
                    .responseFormat(SpeechCreateParams.ResponseFormat.WAV)
                    .build();

            byte[] audioBytes;
            try (HttpResponse response = openAiService.audio().speech().create(request);
                 InputStream body = response.body()) {
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new LLMServiceException("OpenAI TTS request failed with HTTP status " + response.statusCode());
                }
                audioBytes = body.readAllBytes();
            }
            playWav(audioBytes);
        } catch (Exception e) {
            Throwable root = e;
            while (root.getCause() != null) root = root.getCause();
            String rootMessage = root.getMessage();
            if (rootMessage == null || rootMessage.isBlank()) {
                rootMessage = "Unknown TTS error";
            }
            throw new LLMServiceException("Failed to generate OpenAI TTS audio"
                    + "\nRoot cause: " + root.getClass().getSimpleName() + ": " + rootMessage, e);
        }
    }

    private static String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "https://api.openai.com/v1";
        }
        String normalized = baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized.endsWith("/v1") ? normalized : normalized + "/v1";
    }

    private static void addMessage(ChatCompletionCreateParams.Builder builder, Message message) {
        String role = message.getRole() == null ? "user" : message.getRole().trim().toLowerCase();
        if ("system".equals(role)) {
            builder.addSystemMessage(message.getMessage());
        } else if ("assistant".equals(role)) {
            builder.addAssistantMessage(message.getMessage());
        } else {
            builder.addUserMessage(message.getMessage());
        }
    }

    private static SpeechCreateParams.Voice resolveVoice(String selectedVoiceId) {
        if (selectedVoiceId == null) {
            return SpeechCreateParams.Voice.ALLOY;
        }
        return switch (selectedVoiceId.trim().toLowerCase()) {
            case "ash" -> SpeechCreateParams.Voice.ASH;
            case "ballad" -> SpeechCreateParams.Voice.BALLAD;
            case "coral" -> SpeechCreateParams.Voice.CORAL;
            case "echo" -> SpeechCreateParams.Voice.ECHO;
            case "sage" -> SpeechCreateParams.Voice.SAGE;
            case "shimmer" -> SpeechCreateParams.Voice.SHIMMER;
            case "verse" -> SpeechCreateParams.Voice.VERSE;
            case "marin" -> SpeechCreateParams.Voice.MARIN;
            case "cedar" -> SpeechCreateParams.Voice.CEDAR;
            default -> SpeechCreateParams.Voice.ALLOY;
        };
    }

    private void playWav(byte[] audioBytes) throws Exception {
        if (audioBytes == null || audioBytes.length == 0) {
            throw new LLMServiceException("OpenAI TTS returned empty audio");
        }
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(audioBytes);
             AudioInputStream sourceStream = AudioSystem.getAudioInputStream(byteArrayInputStream)) {
            AudioFormat sourceFormat = sourceStream.getFormat();
            AudioFormat targetFormat = sourceFormat;
            AudioInputStream playbackStream = sourceStream;

            boolean requiresPcm16 = !AudioFormat.Encoding.PCM_SIGNED.equals(sourceFormat.getEncoding())
                    || sourceFormat.getSampleSizeInBits() != 16;
            if (requiresPcm16) {
                AudioFormat pcm16 = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        sourceFormat.getSampleRate(),
                        16,
                        sourceFormat.getChannels(),
                        sourceFormat.getChannels() * 2,
                        sourceFormat.getSampleRate(),
                        false
                );
                if (AudioSystem.isConversionSupported(pcm16, sourceFormat)) {
                    targetFormat = pcm16;
                    playbackStream = AudioSystem.getAudioInputStream(pcm16, sourceStream);
                }
            }

            try (AudioInputStream playableStream = playbackStream) {
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, targetFormat);
                SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
                activeLines.add(line);
                try {
                    line.open(targetFormat);
                    line.start();
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = playableStream.read(buffer, 0, buffer.length)) != -1) {
                        if (bytesRead > 0) {
                            line.write(buffer, 0, bytesRead);
                        }
                    }
                    line.drain();
                } finally {
                    line.stop();
                    line.close();
                    activeLines.remove(line);
                }
            }
        }
    }

}
