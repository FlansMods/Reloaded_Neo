package com.flansmod.common.abilities;

import com.flansmod.common.FlansModConfig;
import com.flansmod.common.actions.contexts.ActionGroupContext;
import com.flansmod.common.actions.contexts.GunContext;
import com.flansmod.common.actions.contexts.TriggerContext;
import com.flansmod.common.actions.contexts.TargetsContext;
import com.flansmod.common.gunshots.EPlayerHitArea;
import com.flansmod.common.gunshots.PlayerHitResult;
import com.flansmod.common.types.Constants;
import com.flansmod.common.types.abilities.elements.AbilityEffectDefinition;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class AbilityEffectApplyDamage implements IAbilityEffect
{
	private final StatHolder ImpactDamage;
	private final boolean PreventDamageCooldown;

	public AbilityEffectApplyDamage(@Nonnull AbilityEffectDefinition def)
	{
		ImpactDamage = new StatHolder(Constants.STAT_IMPACT_DAMAGE, def);
		PreventDamageCooldown = def.ModifyBoolean(Constants.STAT_PREVENT_DAMAGE_COOLDOWN, true);
	}

	@Override
	public void TriggerServer(@Nonnull ActionGroupContext actionGroup, @Nonnull TriggerContext trigger, @Nonnull TargetsContext targets, @Nullable AbilityStack stacks)
	{
		// Server config hook
		float globalDmgMulti = FlansModConfig.GlobalDamageMultiplier.get().floatValue();
		float globalHeadshotMulti = FlansModConfig.GlobalHeadshotMultiplier.get().floatValue();
		// ------------------

		DamageSource dmgSource = actionGroup.Gun.CreateDamageSource();
		targets.ForEachEntity((triggerOn) ->
			{
				float headshotMulti = (trigger.Hit instanceof PlayerHitResult playerHit && playerHit.GetHitbox().area == EPlayerHitArea.HEAD)
					? globalHeadshotMulti
					: 1.0f;

				triggerOn.hurt(dmgSource, globalDmgMulti * headshotMulti * DamageAmount(actionGroup, stacks));
				if(PreventDamageCooldown && triggerOn instanceof LivingEntity living)
				{
					living.hurtTime = 0;
					living.hurtDuration = 0;
					living.invulnerableTime = 0;
				}
			});
	}

	public float DamageAmount(@Nonnull ActionGroupContext actionGroup, @Nullable AbilityStack stacks)
	{
		return ImpactDamage.Get(actionGroup, stacks);
	}
}
