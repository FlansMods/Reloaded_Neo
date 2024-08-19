package com.flansmod.common.abilities;

import com.flansmod.common.actions.contexts.ActionGroupContext;
import com.flansmod.common.actions.contexts.TriggerContext;
import com.flansmod.common.actions.contexts.TargetsContext;
import com.flansmod.common.types.Constants;
import com.flansmod.common.types.abilities.elements.AbilityEffectDefinition;
import com.flansmod.util.Maths;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

public class AbilityEffectApplyMobEffect implements IAbilityEffect
{
	@Nonnull
	public final Optional<Holder.Reference<MobEffect>> Effect;

	@Nonnull
	private final StatHolder PotionMultiplier;

	public AbilityEffectApplyMobEffect(@Nonnull AbilityEffectDefinition def)
	{
		Effect = BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse(def.ModifyString(Constants.KEY_MOB_EFFECT_ID, "")));
		PotionMultiplier = new StatHolder(Constants.STAT_POTION_MULTIPLIER, def);
	}

	@Override
	public void TriggerServer(@Nonnull ActionGroupContext actionGroup, @Nonnull TriggerContext trigger, @Nonnull TargetsContext targets, @Nullable AbilityStack stacks)
	{
		Effect.ifPresent(mobEffectReference -> targets.ForEachEntity((entity) ->
		{
			if (entity instanceof LivingEntity living)
			{
				int decayTicks = stacks == null ? 20 : stacks.GetDecayTimeTicks(actionGroup.Gun);
				living.addEffect(
					new MobEffectInstance(mobEffectReference, decayTicks, Maths.Ceil(PotionMultiplier.Get(actionGroup, stacks)) - 1),
					actionGroup.Gun.GetShooter().Owner());
			}
		}));
	}
}
