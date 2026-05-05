package me.sailex.secondbrain.event

import net.minecraft.server.network.ServerPlayerEntity

interface EventHandler {

    fun onEvent(prompt: String, sender: ServerPlayerEntity?)
    fun stopService()
    fun queueIsEmpty(): Boolean
    fun isCommandRunning(): Boolean
}