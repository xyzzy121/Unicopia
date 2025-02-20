package com.minelittlepony.unicopia.client.gui.spellbook;

import java.util.List;

import org.joml.Vector4f;

import com.minelittlepony.common.client.gui.element.Button;
import com.minelittlepony.unicopia.ability.magic.spell.effect.CustomisedSpellType;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.sound.SoundManager;

public class EquippedSpellSlot extends Button {

    protected final ItemRenderer itemRenderer = MinecraftClient.getInstance().getItemRenderer();

    private final CustomisedSpellType<?> spell;

    public EquippedSpellSlot(int x, int y, CustomisedSpellType<?> spell) {
        super(x, y, 16, 16);
        this.spell = spell;
        getStyle().setTooltip(() -> {
            if (spell.isEmpty()) {
                return List.of();
            }
            return spell.getDefaultStack().getTooltip(MinecraftClient.getInstance().player, TooltipContext.Default.BASIC);
        });
    }

    @Override
    public void renderButton(DrawContext context, int mouseX, int mouseY, float tickDelta) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.enableBlend();

        context.drawTexture(SpellbookScreen.SLOT, getX() - 8, getY() - 8, 0, 0, 32, 32, 32, 32);

        Vector4f pos = new Vector4f(getX(), getY(), 0, 1);
        pos.mul(context.getMatrices().peek().getPositionMatrix());

        if (spell.isEmpty()) {
            RenderSystem.setShaderColor(1, 1, 1, 0.3F);
            context.drawTexture(SpellbookScreen.GEM, getX(), getY(), 0, 0, 16, 16, 16, 16);
            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1, 1, 1, 1);
        } else {
            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1, 1, 1, 1);
            drawItem(context, (int)pos.x, (int)pos.y);
        }
        if (isHovered()) {
            HandledScreen.drawSlotHighlight(context, getX(), getY(), 0);
        }
    }

    protected void drawItem(DrawContext context, int x, int y) {
        context.drawItem(spell.getDefaultStack(), x, y);
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
    }
}
