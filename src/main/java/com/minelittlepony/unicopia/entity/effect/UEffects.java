package com.minelittlepony.unicopia.entity.effect;

import com.minelittlepony.unicopia.Unicopia;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.registry.Registry;
import net.minecraft.registry.Registries;

public interface UEffects {
    StatusEffect FOOD_POISONING = register("food_poisoning", new FoodPoisoningStatusEffect(3484199));
    StatusEffect SUN_BLINDNESS = register("sun_blindness", new SunBlindnessStatusEffect(0x886F0F));
    StatusEffect CORRUPT_INFLUENCE = register("corrupt_influence", new CorruptInfluenceStatusEffect(0x00FF00));
    StatusEffect PARALYSIS = register("paralysis", new StatusEffect(StatusEffectCategory.HARMFUL, 0) {});
    StatusEffect BUTTER_FINGERS = register("butter_fingers", new ButterfingersStatusEffect(0x888800));

    private static StatusEffect register(String name, StatusEffect effect) {
        return Registry.register(Registries.STATUS_EFFECT, Unicopia.id(name), effect);
    }

    static void bootstrap() {}
}
