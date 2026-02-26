package me.sailex.secondbrain.commands;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import lombok.AllArgsConstructor;
import me.sailex.secondbrain.common.NPCService;
import me.sailex.secondbrain.config.ConfigProvider;
import me.sailex.secondbrain.config.NPCConfig;
import me.sailex.secondbrain.util.LogUtil;
import net.minecraft.server.command.ServerCommandSource;

import java.util.Optional;

@AllArgsConstructor
public class NPCUnlockCommand {

	private final NPCService npcService;
	private final ConfigProvider configProvider;

	public LiteralArgumentBuilder<ServerCommandSource> getCommand() {
		return literal("unlock")
				.requires(/*? >=1.21.11 {*/ source -> net.minecraft.server.command.CommandManager.MODERATORS_CHECK.allows(source.getPermissions()) /*?} else {*/ source -> source.hasPermissionLevel(2) /*?}*/)
				.then(argument("npcName", StringArgumentType.string())
						.suggests((context, builder) -> {
							configProvider.getNpcConfigs().stream()
									.map(NPCConfig::getNpcName).forEach(builder::suggest);
							return builder.buildFuture();
						}).then(argument("memoryId", StringArgumentType.string())
								.suggests((context, builder) -> {
									String npcName = StringArgumentType.getString(context, "npcName");
									Optional<NPCConfig> configOpt = configProvider.getNpcConfigByName(npcName);
									configOpt.ifPresent(config -> config.getMemoryFragments()
											.forEach(fragment -> builder.suggest(fragment.getId())));
									return builder.buildFuture();
								})
								.executes(this::unlockMemory)));
	}

	private int unlockMemory(CommandContext<ServerCommandSource> context) {
		String npcName = StringArgumentType.getString(context, "npcName");
		String memoryId = StringArgumentType.getString(context, "memoryId");
		NPCService.MemoryUnlockStatus status = npcService.unlockMemoryForNpc(npcName, memoryId);

		switch (status) {
			case SUCCESS -> {
				context.getSource().sendFeedback(
						() -> LogUtil.formatInfo("Unlocked memory '" + memoryId + "' for NPC '" + npcName + "'"),
						false
				);
				return 1;
			}
			case NPC_NOT_FOUND -> {
				context.getSource().sendFeedback(
						() -> LogUtil.formatError("NPC '" + npcName + "' not found"),
						false
				);
				return 0;
			}
			case MEMORY_NOT_FOUND -> {
				context.getSource().sendFeedback(
						() -> LogUtil.formatError("Memory fragment '" + memoryId + "' not found for NPC '" + npcName + "'"),
						false
				);
				return 0;
			}
			case ALREADY_UNLOCKED -> {
				context.getSource().sendFeedback(
						() -> LogUtil.formatInfo("Memory fragment '" + memoryId + "' is already unlocked for NPC '" + npcName + "'"),
						false
				);
				return 1;
			}
		}
		return 0;
	}
}
