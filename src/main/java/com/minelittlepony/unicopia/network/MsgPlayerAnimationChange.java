package com.minelittlepony.unicopia.network;

import java.util.UUID;

import com.minelittlepony.unicopia.client.render.PlayerPoser.Animation;
import com.minelittlepony.unicopia.client.render.PlayerPoser.AnimationInstance;
import com.minelittlepony.unicopia.entity.player.Pony;
import com.sollace.fabwork.api.packets.HandledPacket;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;

/**
 * Sent to the client when a player's animation changes.
 */
public record MsgPlayerAnimationChange (
        UUID playerId,
        AnimationInstance animation,
        int duration
    ) implements HandledPacket<PlayerEntity> {

    MsgPlayerAnimationChange(PacketByteBuf buffer) {
        this(buffer.readUuid(), new AnimationInstance(buffer.readEnumConstant(Animation.class), buffer.readEnumConstant(Animation.Recipient.class)), buffer.readInt());
    }

    public MsgPlayerAnimationChange(Pony player, AnimationInstance animation, int duration) {
        this(player.asEntity().getUuid(), animation, duration);
    }

    @Override
    public void toBuffer(PacketByteBuf buffer) {
        buffer.writeUuid(playerId);
        buffer.writeEnumConstant(animation.animation());
        buffer.writeEnumConstant(animation.recipient());
        buffer.writeInt(duration);
    }

    @Override
    public void handle(PlayerEntity sender) {
        Pony player = Pony.of(MinecraftClient.getInstance().world.getPlayerByUuid(playerId));
        if (player == null) {
            return;
        }

        player.setAnimation(animation, duration);
    }
}
