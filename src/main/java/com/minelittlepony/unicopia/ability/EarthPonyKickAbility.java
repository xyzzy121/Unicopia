package com.minelittlepony.unicopia.ability;

import java.util.*;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import com.minelittlepony.unicopia.Race;
import com.minelittlepony.unicopia.ability.data.Hit;
import com.minelittlepony.unicopia.ability.data.Pos;
import com.minelittlepony.unicopia.ability.data.tree.TreeType;
import com.minelittlepony.unicopia.client.minelittlepony.MineLPDelegate;
import com.minelittlepony.unicopia.client.render.PlayerPoser.Animation;
import com.minelittlepony.unicopia.entity.Living;
import com.minelittlepony.unicopia.entity.damage.UDamageTypes;
import com.minelittlepony.unicopia.entity.player.Pony;
import com.minelittlepony.unicopia.particle.ParticleUtils;
import com.minelittlepony.unicopia.particle.UParticles;
import com.minelittlepony.unicopia.server.world.BlockDestructionManager;
import com.minelittlepony.unicopia.util.*;

import net.minecraft.block.BeehiveBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BeehiveBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;

/**
 * Earth Pony kicking ability
 */
public class EarthPonyKickAbility implements Ability<Pos> {

    @Override
    public int getWarmupTime(Pony player) {
        return 3;
    }

    @Override
    public int getCooldownTime(Pony player) {
        return 50;
    }

    @Override
    public boolean canUse(Race race) {
        return race.canUseEarth();
    }

    @Override
    public double getCostEstimate(Pony player) {
        double distance = MineLPDelegate.getInstance().getPlayerPonyRace(player.asEntity()).isHuman() ? 6 : -6;

        return TraceHelper.findBlock(player.asEntity(), distance, 1)
                .filter(pos -> TreeType.at(pos, player.asWorld()) != TreeType.NONE)
                .isPresent() ? 3 : 1;
    }

    @Override
    public Optional<Pos> prepareQuickAction(Pony player, ActivationType type) {
        return Optional.of(getDefaultKickLocation(player));
    }

    @Override
    public boolean onQuickAction(Pony player, ActivationType type, Optional<Pos> data) {
        if (type == ActivationType.TAP) {

            if (!player.isClient()) {
                data.ifPresent(kickLocation -> {
                    Vec3d origin = player.getOriginVector();
                    World w = player.asWorld();

                    for (var e : VecHelper.findInRange(player.asEntity(), w, kickLocation.vec(), 2, EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR)) {
                        if (e instanceof LivingEntity entity) {
                            float calculatedStrength = 0.5F * (1 + player.getLevel().getScaled(9));

                            entity.damage(player.damageOf(UDamageTypes.KICK, player), player.asWorld().random.nextBetween(2, 10) + calculatedStrength);
                            entity.takeKnockback(calculatedStrength, origin.x - entity.getX(), origin.z - entity.getZ());
                            Living.updateVelocity(entity);
                            player.subtractEnergyCost(3);
                            player.setAnimation(Animation.KICK, Animation.Recipient.ANYONE);
                            return;
                        }
                    }

                    BlockPos pos = kickLocation.pos();
                    EarthPonyStompAbility.stompBlock(w, pos, 10 * (1 + player.getLevel().getScaled(5)) * w.getBlockState(pos).calcBlockBreakingDelta(player.asEntity(), w, pos));
                    player.setAnimation(Animation.KICK, Animation.Recipient.ANYONE);
                });
            }

            return true;
        }

        if (type == ActivationType.DOUBLE_TAP && player.asEntity().isOnGround() && player.getMagicalReserves().getMana().get() > 40) {
            player.getPhysics().dashForward((float)player.asWorld().random.nextTriangular(3.5F, 0.3F));
            player.subtractEnergyCost(4);
            return true;
        }

        return false;
    }

    @Nullable
    @Override
    public Pos tryActivate(Pony player) {
        return TraceHelper.findBlock(player.asEntity(), 6 * getKickDirection(player), 1)
                .filter(pos -> TreeType.at(pos, player.asWorld()) != TreeType.NONE)
                .map(Pos::new)
                .orElseGet(() -> getDefaultKickLocation(player));
    }

    private int getKickDirection(Pony player) {
        return MineLPDelegate.getInstance().getPlayerPonyRace(player.asEntity()).isHuman() ? 1 : -1;
    }

    private Pos getDefaultKickLocation(Pony player) {
        Vec3d kickVector = player.asEntity().getRotationVector().multiply(1, 0, 1);

        if (MineLPDelegate.getInstance().getPlayerPonyRace(player.asEntity()).isEquine()) {
            kickVector = kickVector.rotateY((float)Math.PI);
        }
        return new Pos(BlockPos.ofFloored(player.getOriginVector().add(kickVector)));
    }

    @Override
    public boolean canApply(Pony player, Pos data) {
        BlockPos pos = data.pos();
        TreeType tree = TreeType.at(pos, player.asWorld());

        return tree == TreeType.NONE || tree.findBase(player.asWorld(), pos)
                .map(base -> tree.countBlocks(player.asWorld(), pos) > 0)
                .orElse(false);
    }

    @Override
    public Hit.Serializer<Pos> getSerializer() {
        return Pos.SERIALIZER;
    }

    @Override
    public void apply(Pony iplayer, Pos data) {
        BlockPos pos = data.pos();
        TreeType tree = TreeType.at(pos, iplayer.asWorld());

        iplayer.setAnimation(Animation.KICK, Animation.Recipient.ANYONE);
        iplayer.subtractEnergyCost(tree == TreeType.NONE ? 1 : 3);

        if (tree == TreeType.NONE) {
            return;
        }

        ParticleUtils.spawnParticle(iplayer.asWorld(), UParticles.GROUND_POUND, data.vec(), Vec3d.ZERO);

        PlayerEntity player = iplayer.asEntity();

        if (BlockDestructionManager.of(player.getWorld()).getBlockDestruction(pos) + 4 >= BlockDestructionManager.MAX_DAMAGE) {
            if (player.getWorld().random.nextInt(30) == 0) {
                tree.traverse(player.getWorld(), pos, (w, state, p, recurseLevel) -> {
                    if (recurseLevel < 5) {
                        w.breakBlock(p, true);
                    } else {
                        Block.dropStacks(w.getBlockState(p), w, p);
                        w.setBlockState(p, Blocks.AIR.getDefaultState(), 3);
                    }
                });
            }

            iplayer.subtractEnergyCost(3);
        } else {
            int cost = dropApples(player, pos);

            if (cost > 0) {
                iplayer.subtractEnergyCost(cost / 7F);
            }
        }
    }

    @Override
    public void preApply(Pony player, AbilitySlot slot) {
        player.getMagicalReserves().getExertion().add(40);
    }

    @Override
    public void postApply(Pony player, AbilitySlot slot) {
    }

    private int dropApples(PlayerEntity player, BlockPos pos) {
        TreeType tree = TreeType.at(pos, player.getWorld());

        if (tree.countBlocks(player.getWorld(), pos) > 0) {
            List<ItemEntity> capturedDrops = new ArrayList<>();

            tree.traverse(player.getWorld(), pos, (world, state, position, recurse) -> {
                affectBlockChange(player, position);
            }, (world, state, position, recurse) -> {
                affectBlockChange(player, position);
                List<ItemEntity> drops = buckBlock(tree, state, world, position)
                        .filter(i -> !i.isEmpty())
                        .map(stack -> createDrop(stack, position, world))
                        .toList();
                if (!drops.isEmpty()) {
                    world.syncWorldEvent(WorldEvents.BLOCK_BROKEN, position, Block.getRawIdFromState(state));
                    capturedDrops.addAll(drops);
                }
            });

            capturedDrops.forEach(player.getWorld()::spawnEntity);

            return capturedDrops.size() / 3;
        }

        return 0;
    }

    private ItemEntity createDrop(ItemStack stack, BlockPos pos, World world) {
        ItemEntity entity = new ItemEntity(world,
            pos.getX() + world.random.nextFloat(),
            pos.getY() - 0.5,
            pos.getZ() + world.random.nextFloat(),
            stack
        );
        entity.setToDefaultPickupDelay();
        return entity;
    }

    private Stream<ItemStack> buckBlock(TreeType tree, BlockState treeState, World world, BlockPos position) {

        if (treeState.getBlock() instanceof Buckable buckable) {
            return buckable.onBucked((ServerWorld)world, treeState, position).stream();
        }

        BlockPos down = position.down();
        BlockState below = world.getBlockState(down);

        if (below.isAir()) {
            return Stream.of(tree.pickRandomStack(world.random, treeState));
        }

        if (below.getBlock() instanceof Buckable buckable) {
            return buckable.onBucked((ServerWorld)world, below, down).stream();
        }

        return Stream.empty();
    }

    private void affectBlockChange(PlayerEntity player, BlockPos position) {
        BlockDestructionManager.of(player.getWorld()).damageBlock(position, 4);

        PosHelper.all(position, p -> {
            BlockState s = player.getWorld().getBlockState(p);

            if (s.getBlock() instanceof BeehiveBlock) {
                if (player.getWorld().getBlockEntity(p) instanceof BeehiveBlockEntity hive) {
                    hive.angerBees(player, s, BeehiveBlockEntity.BeeState.EMERGENCY);
                }

                player.getWorld().updateComparators(position, s.getBlock());

                Box area = new Box(position).expand(8, 6, 8);
                List<BeeEntity> nearbyBees = player.getWorld().getNonSpectatingEntities(BeeEntity.class, area);

                if (!nearbyBees.isEmpty()) {
                    List<PlayerEntity> nearbyPlayers = player.getWorld().getNonSpectatingEntities(PlayerEntity.class, area);
                    int i = nearbyPlayers.size();

                    for (BeeEntity bee : nearbyBees) {
                        if (bee.getTarget() == null) {
                            bee.setTarget(nearbyPlayers.get(player.getWorld().random.nextInt(i)));
                        }
                    }
                }
            }
        }, PosHelper.HORIZONTAL);
    }

    public interface Buckable {
        List<ItemStack> onBucked(ServerWorld world, BlockState state, BlockPos pos);
    }
}
