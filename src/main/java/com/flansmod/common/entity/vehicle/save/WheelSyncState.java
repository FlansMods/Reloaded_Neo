package com.flansmod.common.entity.vehicle.save;

import com.flansmod.common.entity.vehicle.IVehicleSaveNode;
import com.flansmod.common.entity.vehicle.VehicleEntity;
import com.flansmod.util.Transform;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;

public class WheelSyncState implements IVehicleSaveNode
{
	public static final WheelSyncState INVALID = new WheelSyncState();

	@Nonnull
	public Transform LocationCurrent;
	@Nonnull
	public Transform LocationPrevious;

	public WheelSyncState()
	{
		LocationCurrent = Transform.Identity();
		LocationPrevious = Transform.Identity();
	}

	public void Load(@Nonnull VehicleEntity vehicle, @Nonnull CompoundTag tags)
	{
		if(tags.contains("loc"))
		{
			LocationCurrent = Transform.FromPosAndOriTag(tags.getCompound("loc"));
			LocationPrevious = LocationCurrent;
		}
	}

	@Nonnull
	public CompoundTag Save(@Nonnull VehicleEntity vehicle)
	{
		CompoundTag tags = new CompoundTag();
		tags.put("loc", LocationCurrent.ToPosAndOriTag());
		return tags;
	}
}
