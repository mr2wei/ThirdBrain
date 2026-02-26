package me.sailex.secondbrain.client.gui.screen;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.CheckboxComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.TextAreaComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import me.sailex.secondbrain.client.networking.ClientNetworkManager;
import me.sailex.secondbrain.config.NPCConfig;
import me.sailex.secondbrain.networking.packet.UpdateNpcConfigPacket;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.IdentityHashMap;

import static me.sailex.secondbrain.SecondBrain.MOD_ID;

public class NPCMemoryFragmentsScreen extends ConfigScreen<NPCConfig> {

    private static final Identifier ID = Identifier.of(MOD_ID, "npcfragments");
    private static final int WIDE_INPUT_WIDTH = 42;
    private static final int SINGLE_LINE_INPUT_HEIGHT = 9;
    private static final int PROMPT_INPUT_HEIGHT = 18;
    private final IdentityHashMap<NPCConfig.MemoryFragment, Boolean> expandedFragments = new IdentityHashMap<>();
    private final Screen parentScreen;

    public NPCMemoryFragmentsScreen(ClientNetworkManager networkManager, NPCConfig npcConfig, Screen parentScreen) {
        super(networkManager, npcConfig, true, ID);
        this.parentScreen = parentScreen;
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        FlowLayout panel = rootComponent.childById(FlowLayout.class, "panel");
        panel.childById(LabelComponent.class, "title")
                .text(Text.of("Fragments for '" + config.getNpcName() + "'"));

        FlowLayout fragmentList = panel.childById(FlowLayout.class, "fragmentList");
        renderFragments(fragmentList);

        rootComponent.childById(ButtonComponent.class, "add_fragment").onPress(button -> {
            NPCConfig.MemoryFragment newFragment = new NPCConfig.MemoryFragment(
                    generateNextMemoryId(),
                    "",
                    false
            );
            config.getMemoryFragments().add(newFragment);
            expandedFragments.put(newFragment, true);
            renderFragments(fragmentList);
        });

        onPressSaveButton(rootComponent, button -> {
            networkManager.sendPacket(new UpdateNpcConfigPacket(config));
            close();
        });
        rootComponent.childById(ButtonComponent.class, "cancel").onPress(button -> client.setScreen(parentScreen));
    }

    private void renderFragments(FlowLayout fragmentList) {
        fragmentList.clearChildren();
        expandedFragments.keySet().removeIf(fragment -> !config.getMemoryFragments().contains(fragment));
        if (config.getMemoryFragments().isEmpty()) {
            fragmentList.child(Components.label(Text.of("No memory fragments yet.")).shadow(true));
            return;
        }

        for (int i = 0; i < config.getMemoryFragments().size(); i++) {
            int fragmentIndex = i;
            NPCConfig.MemoryFragment fragment = config.getMemoryFragments().get(i);

            FlowLayout fragmentCard = Containers.verticalFlow(Sizing.fill(96), Sizing.content());
            fragmentCard.gap(4);
            fragmentCard.padding(Insets.of(6));
            fragmentCard.surface(Surface.DARK_PANEL);
            boolean isExpanded = expandedFragments.getOrDefault(fragment, true);
            fragmentCard.child(Components.button(
                    Text.of((isExpanded ? "[-] " : "[+] ") + "Fragment " + (i + 1)),
                    button -> {
                        expandedFragments.put(fragment, !expandedFragments.getOrDefault(fragment, true));
                        renderFragments(fragmentList);
                    }
            ).sizing(Sizing.fill(100), Sizing.content()));

            if (isExpanded) {
                FlowLayout fragmentDetails = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
                fragmentDetails.gap(4);

                fragmentDetails.child(Components.label(Text.of("ID")).shadow(true));
                TextAreaComponent idInput = Components.textArea(
                        Sizing.fill(WIDE_INPUT_WIDTH),
                        Sizing.fill(SINGLE_LINE_INPUT_HEIGHT)
                ).text(fragment.getId());
                idInput.onChanged().subscribe(fragment::setId);
                fragmentDetails.child(idInput);

                fragmentDetails.child(Components.label(Text.of("Prompt")).shadow(true));
                TextAreaComponent promptInput = Components.textArea(
                        Sizing.fill(WIDE_INPUT_WIDTH),
                        Sizing.fill(PROMPT_INPUT_HEIGHT)
                ).text(fragment.getPrompt());
                promptInput.onChanged().subscribe(fragment::setPrompt);
                fragmentDetails.child(promptInput);

                CheckboxComponent unlocked = Components.checkbox(Text.of("Unlocked"))
                        .checked(fragment.isUnlocked())
                        .onChanged(fragment::setUnlocked);
                fragmentDetails.child(unlocked);

                fragmentDetails.child(Components.button(Text.of("Remove Fragment"), button -> {
                    expandedFragments.remove(fragment);
                    config.getMemoryFragments().remove(fragmentIndex);
                    renderFragments(fragmentList);
                }));
                fragmentCard.child(fragmentDetails);
            }

            fragmentList.child(fragmentCard);
        }
    }

    private String generateNextMemoryId() {
        int nextId = 1;
        while (config.getMemoryFragment("memory_" + nextId).isPresent()) {
            nextId++;
        }
        return "memory_" + nextId;
    }
}
