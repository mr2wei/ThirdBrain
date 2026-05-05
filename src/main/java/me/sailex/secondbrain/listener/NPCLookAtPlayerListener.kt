package me.sailex.secondbrain.listener

import me.sailex.altoclef.multiversion.EntityVer
import me.sailex.secondbrain.model.NPC
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.entity.Entity
//? >=1.21.8 {
/*import net.minecraft.network.packet.s2c.play.EntityPositionSyncS2CPacket
*///?} else {
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket
//?}
import net.minecraft.network.packet.s2c.play.EntitySetHeadYawS2CPacket
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.math.MathHelper
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.sqrt

class NPCLookAtPlayerListener(
    npcs: Map<UUID, NPC>
) : AEventListener(npcs) {

    override fun register() {
        ServerTickEvents.END_SERVER_TICK.register {
            npcs.values.forEach(::lookAtNearestPlayerIfIdle)
        }
    }

    private fun lookAtNearestPlayerIfIdle(npc: NPC) {
        if (npc.eventHandler.isCommandRunning()) {
            return
        }

        val target = findNearestPlayerInConversationRange(npc) ?: return
        rotateNpcToward(npc.entity, target)
    }

    private fun findNearestPlayerInConversationRange(npc: NPC): ServerPlayerEntity? {
        val npcEntity = npc.entity
        val npcWorld = EntityVer.getWorld(npcEntity)
        val range = npc.config.conversationRange.toDouble()
        val rangeSquared = range * range

        return npcWorld.server!!.playerManager.playerList
            .asSequence()
            .filter { player -> player.uuid != npcEntity.uuid }
            .filter { player -> EntityVer.getWorld(player) == npcWorld }
            .filter { player -> !player.isSpectator }
            .filter { player -> npcEntity.squaredDistanceTo(player) <= rangeSquared }
            .minByOrNull { player -> npcEntity.squaredDistanceTo(player) }
    }

    private fun rotateNpcToward(npcEntity: ServerPlayerEntity, target: Entity) {
        val dx = target.x - npcEntity.x
        val dy = target.eyeY - npcEntity.eyeY
        val dz = target.z - npcEntity.z
        val horizontalDistance = sqrt(dx * dx + dz * dz)

        val yaw = MathHelper.wrapDegrees((atan2(dz, dx) * 180.0 / Math.PI).toFloat() - 90.0f)
        val pitch = MathHelper.wrapDegrees(-(atan2(dy, horizontalDistance) * 180.0 / Math.PI).toFloat())

        npcEntity.yaw = yaw
        npcEntity.pitch = pitch
        npcEntity.headYaw = yaw
        npcEntity.bodyYaw = yaw

        val npcWorld = EntityVer.getWorld(npcEntity)
        val playerManager = npcWorld.server!!.playerManager
        val dimensionKey = npcWorld.registryKey
        playerManager.sendToDimension(
            EntitySetHeadYawS2CPacket(npcEntity, (yaw * 256.0f / 360.0f).toInt().toByte()),
            dimensionKey
        )
        //? >=1.21.8 {
        /*playerManager.sendToDimension(EntityPositionSyncS2CPacket.create(npcEntity), dimensionKey)
        *///?} else {
        playerManager.sendToDimension(EntityPositionS2CPacket(npcEntity), dimensionKey)
        //?}
    }
}
