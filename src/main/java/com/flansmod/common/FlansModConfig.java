package com.flansmod.common;

import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec;

import javax.annotation.Nonnull;

public class FlansModConfig
{
	public static final IConfigSpec GeneralConfig;

	public static ModConfigSpec.BooleanValue AllowBulletsBreakBlocks;
	public static ModConfigSpec.BooleanValue AllowBulletsCreateExplosions;
	public static ModConfigSpec.BooleanValue AllowBulletsCreateFire;

	public static ModConfigSpec.BooleanValue AllowSummonNpc;
	public static ModConfigSpec.DoubleValue SummonNpcMinDistance;
	public static ModConfigSpec.DoubleValue SummonNpcExtraCooldown;

	public static ModConfigSpec.BooleanValue AllowPainting;
	public static ModConfigSpec.IntValue AdditionalPaintCanCost;
	public static ModConfigSpec.BooleanValue AllowMagazineModifying;
	public static ModConfigSpec.IntValue AdditionalMagazineModifyCost;
	public static ModConfigSpec.BooleanValue AllowGunCrafting;
	public static ModConfigSpec.BooleanValue AllowPartCrafting;


	public static ModConfigSpec.BooleanValue AllowShootActions;
	public static ModConfigSpec.BooleanValue AllowRaycastActions;
	public static ModConfigSpec.BooleanValue AllowLaserActions;

	public static ModConfigSpec.DoubleValue GlobalDamageMultiplier;
	public static ModConfigSpec.DoubleValue GlobalHealMultiplier;
	public static ModConfigSpec.DoubleValue GlobalFireDurationMultiplier;
	public static ModConfigSpec.DoubleValue GlobalRepairMultiplier;
	public static ModConfigSpec.DoubleValue GlobalHeadshotMultiplier;


	static
	{
		ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
		Init(builder);
		GeneralConfig = builder.build();
	}

	private static void Init(@Nonnull ModConfigSpec.Builder builder)
	{
		builder.push("World Settings");
		AllowBulletsBreakBlocks = builder.define("allow_bullet_break_blocks", true);
		AllowBulletsCreateExplosions = builder.define("allow_bullet_create_explosions", true);
		AllowBulletsCreateFire = builder.define("allow_bullet_create_fire", true);
		AllowSummonNpc = builder.define("allow_summon_npc", true);
		SummonNpcMinDistance = builder.defineInRange("summon_npc_min_distance", 400d, 0d, 1000d);
		SummonNpcExtraCooldown = builder.defineInRange("summon_npc_extra_cooldown", 0d, 0d, 10000d);
		builder.pop();

		builder.push("Crafting Settings");
		AllowPainting = builder.define("allow_painting", true);
		AdditionalPaintCanCost = builder.defineInRange("additional_paint_can_cost", 0, 0, 100);
		AllowMagazineModifying = builder.define("allow_magazine_modifying", true);
		AdditionalMagazineModifyCost = builder.defineInRange("additional_magazine_modify_cost", 0, 0, 100);
		AllowGunCrafting = builder.define("allow_gun_crafting", true);
		AllowPartCrafting = builder.define("allow_part_crafting", true);
		builder.pop();

		builder.push("Actions");
		AllowShootActions = builder.define("allow_shoot_actions", true);
		AllowRaycastActions = builder.define("allow_raycast_actions", true);
		AllowLaserActions = builder.define("allow_laser_actions", true);
		GlobalDamageMultiplier = builder.defineInRange("global_damage_multiplier", 1d, 0d, 100d);
		GlobalHealMultiplier = builder.defineInRange("global_heal_multiplier", 1d, 0d, 100d);
		GlobalFireDurationMultiplier = builder.defineInRange("global_fire_duration_multiplier", 1d, 0d, 100d);
		GlobalRepairMultiplier = builder.defineInRange("global_repair_multiplier", 1d, 0d, 100d);
		GlobalHeadshotMultiplier = builder.defineInRange("global_headshot_multiplier", 1.4d, 0d, 100d);
		builder.pop();
	}
}
