package com.flansmod.common.actions.contexts;

import com.flansmod.common.actions.ActionStack;
import com.flansmod.common.actions.stats.IModifierBaker;
import com.flansmod.common.actions.stats.StatCalculationContext;
import com.flansmod.common.types.elements.ModifierDefinition;
import net.minecraft.world.Container;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.function.BiConsumer;

public class GunContextItem extends GunContext
{
	public GunContextItem(@Nonnull ItemStack stack)
	{
		super(stack);
	}
	// We don't even know where this ItemStack lives, no-op
	@Override
	public void OnItemStackChanged(@Nonnull ItemStack stack) { }
	@Override
	@Nonnull
	public EItemStackLinkage CheckItemStackLink() { return EItemStackLinkage.NotConnected; }
	@Override
	@Nonnull
	public ItemStack GetLinkedItemStack() { return ItemStack.EMPTY; }
	@Override
	@Nullable
	public DamageSource CreateDamageSource() { return null; }
	@Override
	@Nonnull
	public ShooterContext GetShooter() { return ShooterContext.INVALID; }
	@Override
	@Nullable
	public Container GetAttachedInventory() { return null; }
	@Override
	public boolean CanPerformActions() { return false; }
	@Override
	@Nonnull
	public ActionStack GetActionStack() { return ActionStack.Invalid; }
	@Override
	public boolean CanPerformTwoHandedAction() { return false; }

	@Override
	public int hashCode()
	{
		return Objects.hash(
			Stack.getCount(),
			Item.getId(Stack.getItem()),
			Stack.getDamageValue(),
			Stack.getTags());
	}
	@Override
	public boolean equals(Object other)
	{
		if(other == this) return true;
		if(other instanceof GunContextItem otherContext)
		{
			return ItemStack.isSameItemSameComponents(Stack, otherContext.Stack);
		}
		return false;
	}
	// There are no modifiers to apply right now
	@Override
	public void BakeModifiers(@Nonnull IModifierBaker baker) {}
	@Override
	public String toString()
	{
		return "Item ["+Stack+"]";
	}
}
