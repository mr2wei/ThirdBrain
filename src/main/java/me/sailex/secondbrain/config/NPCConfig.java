package me.sailex.secondbrain.config;

import me.sailex.secondbrain.constant.Instructions;
import me.sailex.secondbrain.llm.LLMType;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import io.wispforest.endec.Endec;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.impl.StructEndecBuilder;

public class NPCConfig implements Configurable {

	private String npcName = "Steve";
	private UUID uuid = UUID.randomUUID();
	private boolean isActive = true;
	private String llmCharacter = Instructions.DEFAULT_CHARACTER_TRAITS;
	private LLMType llmType = LLMType.OLLAMA;
    private String llmModel = "llama3.2";
	private String voiceId = "not set";
	private String skinUrl = "";
	private List<ZoneBehavior> zoneBehaviors = new ArrayList<>();

	private boolean isTTS = false;

	public NPCConfig() {}

	public NPCConfig(String npcName) {
		this.npcName = npcName;
	}

    public NPCConfig(
		String npcName,
		String uuid,
		boolean isActive,
		String llmCharacter,
		LLMType llmType,
        String llmModel,
		boolean isTTS,
		String voiceId,
		String skinUrl,
		List<ZoneBehavior> zoneBehaviors
	) {
		this.npcName = npcName;
		this.uuid = UUID.fromString(uuid);
		this.isActive = isActive;
		this.llmCharacter = llmCharacter;
		this.llmType = llmType;
        this.llmModel = llmModel;
		this.isTTS = isTTS;
		this.voiceId = voiceId;
		this.skinUrl = skinUrl;
		setZoneBehaviors(zoneBehaviors);
	}

	public static class Builder {

		private final NPCConfig npcConfig;

		public Builder(String npcName) {
			this.npcConfig = new NPCConfig(npcName);
		}

		public Builder uuid(UUID uuid) {
			npcConfig.setUuid(uuid);
			return this;
		}

		public Builder llmDefaultPrompt(String llmDefaultPrompt) {
			npcConfig.setLlmCharacter(llmDefaultPrompt);
			return this;
		}

		public Builder llmType(LLMType llmType) {
			npcConfig.setLlmType(llmType);
			return this;
		}

		public Builder voiceId(String voiceId) {
			npcConfig.setVoiceId(voiceId);
			return this;
		}

		public Builder skinUrl(String skinUrl) {
			npcConfig.setSkinUrl(skinUrl);
			return this;
		}

		public NPCConfig build() {
			return npcConfig;
		}

	}

	public static Builder builder(String npcName) {
		return new Builder(npcName);
	}

	public String getNpcName() {
		return npcName;
	}

	public boolean isActive() {
		return isActive;
	}

	public String getLlmCharacter() {
		return llmCharacter;
	}

	public LLMType getLlmType() {
		return llmType;
	}

    public String getLlmModel() {
        return llmModel;
    }

    public void setLlmModel(String llmModel) {
        this.llmModel = llmModel;
    }


	public void setLlmCharacter(String llmCharacter) {
		this.llmCharacter = llmCharacter;
	}

	public void setLlmType(LLMType llmType) {
		this.llmType = llmType;
	}

	public void setActive(boolean active) {
		isActive = active;
	}

	public void setNpcName(String npcName) {
		this.npcName = npcName;
	}

	public UUID getUuid() {
		return uuid;
	}

	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}

	public String getVoiceId() {
		return voiceId;
	}

	public void setVoiceId(String voiceId) {
		this.voiceId = voiceId;
	}

	public boolean isTTS() {
		return isTTS;
	}

	public void setTTS(boolean TTS) {
		isTTS = TTS;
	}

	public String getSkinUrl() {
		return skinUrl;
	}

	public void setSkinUrl(String skinUrl) {
		this.skinUrl = skinUrl;
	}

	public List<ZoneBehavior> getZoneBehaviors() {
		if (zoneBehaviors == null) {
			zoneBehaviors = new ArrayList<>();
		}
		return zoneBehaviors;
	}

	public void setZoneBehaviors(List<ZoneBehavior> zoneBehaviors) {
		this.zoneBehaviors = zoneBehaviors == null ? new ArrayList<>() : new ArrayList<>(zoneBehaviors);
	}

	@Override
	public String getConfigName() {
		return npcName.toLowerCase();
	}


    public static final StructEndec<NPCConfig> ENDEC = StructEndecBuilder.of(
			Endec.STRING.fieldOf("npcName", NPCConfig::getNpcName),
			Endec.STRING.fieldOf("uuid", config -> config.getUuid().toString()),
			Endec.BOOLEAN.fieldOf("isActive", NPCConfig::isActive),
			Endec.STRING.fieldOf("llmDefaultPrompt", NPCConfig::getLlmCharacter),
			Endec.forEnum(LLMType.class).fieldOf("llmType", NPCConfig::getLlmType),
            Endec.STRING.fieldOf("llmModel", NPCConfig::getLlmModel),
			Endec.BOOLEAN.fieldOf("isTTS", NPCConfig::isTTS),
			Endec.STRING.fieldOf("voiceId", NPCConfig::getVoiceId),
			Endec.STRING.fieldOf("skinUrl", NPCConfig::getSkinUrl),
			ZoneBehavior.ENDEC.listOf().fieldOf("zoneBehaviors", NPCConfig::getZoneBehaviors),
			NPCConfig::new
	);

    public static NPCConfig deepCopy(NPCConfig config) {
        return new NPCConfig(
                config.npcName,
                config.uuid.toString(),
                config.isActive,
                config.llmCharacter,
                config.llmType,
                config.llmModel,
                config.isTTS,
                config.voiceId,
                config.skinUrl,
				deepCopyZoneBehaviors(config.getZoneBehaviors())
        );
    }

	private static List<ZoneBehavior> deepCopyZoneBehaviors(List<ZoneBehavior> zoneBehaviors) {
		if (zoneBehaviors == null || zoneBehaviors.isEmpty()) {
			return Collections.emptyList();
		}
		List<ZoneBehavior> copied = new ArrayList<>();
		for (ZoneBehavior zoneBehavior : zoneBehaviors) {
			copied.add(new ZoneBehavior(
					zoneBehavior.getName(),
					new ZoneCoordinate(
							zoneBehavior.getFrom().getX(),
							zoneBehavior.getFrom().getY(),
							zoneBehavior.getFrom().getZ()
					),
					new ZoneCoordinate(
							zoneBehavior.getTo().getX(),
							zoneBehavior.getTo().getY(),
							zoneBehavior.getTo().getZ()
					),
					zoneBehavior.getPriority(),
					zoneBehavior.getInstructions()
			));
		}
		return copied;
	}

	@Override
	public String toString() {
		return "NPCConfig{npcName=" + npcName +
				",uuid=" + uuid +
				",isActive=" + isActive +
				",llmType=" + llmType +
				",llmCharacter=" + llmCharacter +
				",voiceId=" + voiceId +
				",zoneBehaviorCount=" + getZoneBehaviors().size() + "}";
	}

	//name for fields for npc config screen
	public static final String NPC_NAME = "Name of the NPC";
	public static final String EDIT_NPC = "Edit '%s'";
	public static final String LLM_CHARACTER = "Characteristics";
	public static final String LLM_TYPE = "Type";
	public static final String LLM_MODEL = "LLM Model";
	public static final String IS_TTS = "Text to Speech";
	public static final String ZONE_SPECIFIC_BEHAVIOUR = "Zone Specific Behaviour";
	public static final String ADD_ZONE = "+ Add Zone";

	public static class ZoneBehavior {
		private String name = "Zone";
		private ZoneCoordinate from = new ZoneCoordinate();
		private ZoneCoordinate to = new ZoneCoordinate();
		private int priority = 0;
		private String instructions = "";

		public ZoneBehavior() {}

		public ZoneBehavior(
				String name,
				ZoneCoordinate from,
				ZoneCoordinate to,
				int priority,
				String instructions
		) {
			this.name = name;
			this.from = from == null ? new ZoneCoordinate() : from;
			this.to = to == null ? new ZoneCoordinate() : to;
			this.priority = priority;
			this.instructions = instructions == null ? "" : instructions;
		}

		public boolean contains(BlockPos position) {
			int minX = Math.min(getFrom().getX(), getTo().getX());
			int maxX = Math.max(getFrom().getX(), getTo().getX());
			int minY = Math.min(getFrom().getY(), getTo().getY());
			int maxY = Math.max(getFrom().getY(), getTo().getY());
			int minZ = Math.min(getFrom().getZ(), getTo().getZ());
			int maxZ = Math.max(getFrom().getZ(), getTo().getZ());
			return position.getX() >= minX && position.getX() <= maxX &&
					position.getY() >= minY && position.getY() <= maxY &&
					position.getZ() >= minZ && position.getZ() <= maxZ;
		}

		public String getName() {
			return name;
		}

		public ZoneCoordinate getFrom() {
			if (from == null) {
				from = new ZoneCoordinate();
			}
			return from;
		}

		public ZoneCoordinate getTo() {
			if (to == null) {
				to = new ZoneCoordinate();
			}
			return to;
		}

		public int getPriority() {
			return priority;
		}

		public String getInstructions() {
			return instructions;
		}

		public void setName(String name) {
			this.name = name == null ? "Zone" : name;
		}

		public void setFrom(ZoneCoordinate from) {
			this.from = from == null ? new ZoneCoordinate() : from;
		}

		public void setTo(ZoneCoordinate to) {
			this.to = to == null ? new ZoneCoordinate() : to;
		}

		public void setPriority(int priority) {
			this.priority = priority;
		}

		public void setInstructions(String instructions) {
			this.instructions = instructions == null ? "" : instructions;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!(obj instanceof ZoneBehavior other)) {
				return false;
			}
			return priority == other.priority &&
					Objects.equals(name, other.name) &&
					Objects.equals(from, other.from) &&
					Objects.equals(to, other.to) &&
					Objects.equals(instructions, other.instructions);
		}

		@Override
		public int hashCode() {
			return Objects.hash(name, from, to, priority, instructions);
		}

		public static final StructEndec<ZoneBehavior> ENDEC = StructEndecBuilder.of(
				Endec.STRING.fieldOf("name", ZoneBehavior::getName),
				ZoneCoordinate.ENDEC.fieldOf("from", ZoneBehavior::getFrom),
				ZoneCoordinate.ENDEC.fieldOf("to", ZoneBehavior::getTo),
				Endec.INT.fieldOf("priority", ZoneBehavior::getPriority),
				Endec.STRING.fieldOf("instructions", ZoneBehavior::getInstructions),
				ZoneBehavior::new
		);
	}

	public static class ZoneCoordinate {
		private int x = 0;
		private int y = 0;
		private int z = 0;

		public ZoneCoordinate() {}

		public ZoneCoordinate(int x, int y, int z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}

		public int getX() {
			return x;
		}

		public int getY() {
			return y;
		}

		public int getZ() {
			return z;
		}

		public void setX(int x) {
			this.x = x;
		}

		public void setY(int y) {
			this.y = y;
		}

		public void setZ(int z) {
			this.z = z;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!(obj instanceof ZoneCoordinate other)) {
				return false;
			}
			return x == other.x && y == other.y && z == other.z;
		}

		@Override
		public int hashCode() {
			return Objects.hash(x, y, z);
		}

		public static final StructEndec<ZoneCoordinate> ENDEC = StructEndecBuilder.of(
				Endec.INT.fieldOf("x", ZoneCoordinate::getX),
				Endec.INT.fieldOf("y", ZoneCoordinate::getY),
				Endec.INT.fieldOf("z", ZoneCoordinate::getZ),
				ZoneCoordinate::new
		);
	}
}
