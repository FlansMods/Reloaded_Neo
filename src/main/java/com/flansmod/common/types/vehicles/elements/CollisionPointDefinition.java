package com.flansmod.common.types.vehicles.elements;

import com.flansmod.common.types.JsonField;
import net.minecraft.world.phys.Vec3;

public class CollisionPointDefinition
{
	@JsonField
	public String attachedTo = "body";
	@JsonField
	public Vec3 offset = Vec3.ZERO;

}
