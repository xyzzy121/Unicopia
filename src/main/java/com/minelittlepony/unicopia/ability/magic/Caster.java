package com.minelittlepony.unicopia.ability.magic;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.minelittlepony.unicopia.EquinePredicates;
import com.minelittlepony.unicopia.Owned;
import com.minelittlepony.unicopia.entity.Physics;
import com.minelittlepony.unicopia.entity.PonyContainer;
import com.minelittlepony.unicopia.network.EffectSync;
import com.minelittlepony.unicopia.particle.ParticleSource;
import com.minelittlepony.unicopia.util.VecHelper;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Interface for any magically capable entities that can cast or persist spells.
 */
public interface Caster<E extends LivingEntity> extends Owned<E>, Levelled, Affine, Magical, ParticleSource {

    Physics getPhysics();

    EffectSync getSpellSlot();

    default void setSpell(@Nullable Spell spell) {
        getSpellSlot().put(spell);
    }

    /**
     * Gets the entity directly responsible for casting.
     */
    @Override
    default Entity getEntity() {
        return getMaster();
    }

    /**
     * Gets the minecraft world
     */
    @Override
    default World getWorld() {
        return getEntity().getEntityWorld();
    }

    /**
     * Returns true if we're executing on the client.
     */
    default boolean isClient() {
        return getWorld().isClient();
    }

    /**
     * Gets the center position where this caster is located.
     */
    default BlockPos getOrigin() {
        return getEntity().getBlockPos();
    }

    default boolean subtractEnergyCost(double amount) {
        getMaster().damage(DamageSource.MAGIC, (int)amount/2);
        return getMaster().getHealth() > 0;
    }

    default Stream<Caster<?>> findAllSpellsInRange(double radius) {
        return findAllSpellsInRange(radius, null);
    }

    default Stream<Caster<?>> findAllSpellsInRange(double radius, @Nullable Predicate<Entity> test) {
        return stream(findAllEntitiesInRange(radius, test == null ? EquinePredicates.IS_CASTER : EquinePredicates.IS_CASTER.and(test)));
    }

    default Stream<Entity> findAllEntitiesInRange(double radius, @Nullable Predicate<Entity> test) {
        return VecHelper.findInRange(getEntity(), getWorld(), getOriginVector(), radius, test).stream();
    }

    default Stream<Entity> findAllEntitiesInRange(double radius) {
        return findAllEntitiesInRange(radius, null);
    }

    static Stream<Caster<?>> stream(Stream<Entity> entities) {
        return entities
                .map(Caster::of)
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    /**
     * Attempts to convert the passed entity into a caster using all the known methods.
     */
    static Optional<Caster<?>> of(@Nullable Entity entity) {
        if (entity instanceof Caster<?>) {
            return Optional.of((Caster<?>)entity);
        }

        if (entity instanceof LivingEntity && !(entity instanceof Magical)) {
            return PonyContainer.of(entity).map(PonyContainer::getCaster);
        }

        return Optional.empty();
    }
}
