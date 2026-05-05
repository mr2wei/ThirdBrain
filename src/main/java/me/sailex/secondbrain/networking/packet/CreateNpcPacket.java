package me.sailex.secondbrain.networking.packet;

import io.wispforest.endec.Endec;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.impl.StructEndecBuilder;
import me.sailex.secondbrain.config.NPCConfig;

public record CreateNpcPacket(NPCConfig npcConfig, boolean runInitialPrompt) {

    public static final StructEndec<CreateNpcPacket> ENDEC = StructEndecBuilder.of(
            NPCConfig.ENDEC.fieldOf("npcConfig", CreateNpcPacket::npcConfig),
            Endec.BOOLEAN.fieldOf("runInitialPrompt", CreateNpcPacket::runInitialPrompt),
            CreateNpcPacket::new
    );

    public CreateNpcPacket(NPCConfig npcConfig) {
        this(npcConfig, true);
    }

    @Override
    public String toString() {
        return "AddNpcPacket{npcConfig=" + npcConfig + ",runInitialPrompt=" + runInitialPrompt + "}";
    }
}
