package com.flansmod.common.types.elements;

import com.flansmod.common.types.JsonField;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import static com.flansmod.common.types.JsonDefinition.InvalidLocation;

public class SoundLODDefinition
{
	@OnlyIn(Dist.CLIENT)
	public ResourceLocation GetSoundLocation()
	{
		return sound;
	}

	@JsonField(AssetPathHint = "sounds/")
	public ResourceLocation sound = InvalidLocation;
	@JsonField
	public float minDistance = 100f;
}
