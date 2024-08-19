package com.flansmod.common.item;

import com.flansmod.common.FlansMod;
import com.flansmod.common.types.bullets.BulletDefinition;
import com.flansmod.common.types.grenades.GrenadeDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;

public class GrenadeItem extends FlanItem
{
	@Override
	public GrenadeDefinition Def() { return FlansMod.GRENADES.Get(DefinitionLocation); }
	public GrenadeItem(@Nonnull ResourceLocation defLoc, @Nonnull Properties properties)
	{
		super(defLoc, properties);
	}

	// Random parameter overrides
	public boolean isEnchantable(ItemStack i) { return false; }
}
