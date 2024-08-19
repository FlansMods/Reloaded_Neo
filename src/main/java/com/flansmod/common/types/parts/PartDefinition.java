package com.flansmod.common.types.parts;

import com.flansmod.common.FlansMod;
import com.flansmod.common.item.PartItem;
import com.flansmod.common.types.JsonDefinition;
import com.flansmod.common.types.JsonField;
import com.flansmod.common.types.abilities.elements.CraftingTraitProviderDefinition;
import com.flansmod.common.types.crafting.EMaterialType;
import com.flansmod.common.types.crafting.MaterialDefinition;
import com.flansmod.common.types.elements.ItemDefinition;
import com.flansmod.common.types.elements.ModifierDefinition;
import com.flansmod.common.types.parts.elements.EngineDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PartDefinition extends JsonDefinition
{
	@Nonnull
	public static final PartDefinition INVALID = new PartDefinition(new ResourceLocation(FlansMod.MODID, "parts/null"));
	public static final String TYPE = "part";
	public static final String FOLDER = "parts";
	@Override
	public String GetTypeName() { return TYPE; }

	public PartDefinition(ResourceLocation resLoc)
	{
		super(resLoc);
	}

	@JsonField
	public boolean canPlaceInMachiningTable = false;
	@JsonField
	public boolean canPlaceInModificationTable = false;

	@JsonField
	public String[] compatiblityTags = new String[] { "mecha", "groundVehicle", "plane" };


	@JsonField
	public ItemDefinition itemSettings = new ItemDefinition();
	@JsonField
	public ModifierDefinition[] modifiers = new ModifierDefinition[0];
	@JsonField
	public CraftingTraitProviderDefinition[] traits = new CraftingTraitProviderDefinition[0];

	// Engine properties
	@JsonField
	public EngineDefinition engine = new EngineDefinition();

	@JsonField(AssetPathHint = "materials/")
	public ResourceLocation material = InvalidLocation;

	@Nullable
	private MaterialDefinition CachedMaterial = null;
	@Nonnull
	public MaterialDefinition GetMaterial()
	{
		if(CachedMaterial == null)
			CachedMaterial = FlansMod.MATERIALS.Get(material);
		return CachedMaterial;
	}

	@Nonnull
	public static MaterialDefinition GetMaterialOfPart(ItemStack stack)
	{
		if(stack.getItem() instanceof PartItem partItem)
			return partItem.Def().GetMaterial();
		return MaterialDefinition.INVALID;
	}
	public static int GetPartTier(ItemStack stack)
	{
		return GetMaterialOfPart(stack).craftingTier;
	}
	@Nonnull
	public static EMaterialType GetPartMaterial(ItemStack stack)
	{
		return GetMaterialOfPart(stack).materialType;
	}
}
