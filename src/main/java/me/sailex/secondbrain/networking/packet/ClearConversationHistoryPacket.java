package me.sailex.secondbrain.networking.packet;

import io.wispforest.endec.Endec;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.impl.StructEndecBuilder;

public record ClearConversationHistoryPacket(boolean clearActiveNpcs) {

    public static final StructEndec<ClearConversationHistoryPacket> ENDEC = StructEndecBuilder.of(
            Endec.BOOLEAN.fieldOf("clearActiveNpcs", ClearConversationHistoryPacket::clearActiveNpcs),
            ClearConversationHistoryPacket::new
    );

    @Override
    public String toString() {
        return "ClearConversationHistoryPacket={clearActiveNpcs=" + clearActiveNpcs + "}";
    }
}
