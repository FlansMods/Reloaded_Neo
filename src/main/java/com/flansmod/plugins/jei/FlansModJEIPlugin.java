package com.flansmod.plugins.jei;

import com.flansmod.common.FlansMod;
import com.flansmod.common.crafting.recipes.GunFabricationRecipe;
import com.flansmod.common.crafting.recipes.PartFabricationRecipe;
import com.flansmod.common.types.crafting.WorkbenchDefinition;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import java.util.List;

@SuppressWarnings("unused")
@JeiPlugin
public class FlansModJEIPlugin implements IModPlugin
{
	private static final ResourceLocation ID = new ResourceLocation(FlansMod.MODID, "jei_plugin");
	@Override
	@Nonnull
	public ResourceLocation getPluginUid() { return ID; }

	private static final RecipeType<PartFabricationRecipe> PART_FABRICATION_RECIPE_TYPE = RecipeType.create(FlansMod.MODID, "part_fabrication", PartFabricationRecipe.class);
	private static final PartCraftingCategory PART_CRAFTING_CATEGORY = new PartCraftingCategory(PART_FABRICATION_RECIPE_TYPE);
	private static final RecipeType<GunFabricationRecipe> GUN_FABRICATION_RECIPE_TYPE = RecipeType.create(FlansMod.MODID, "gun_fabrication", GunFabricationRecipe.class);
	private static final GunCraftingCategory GUN_CRAFTING_CATEGORY = new GunCraftingCategory(GUN_FABRICATION_RECIPE_TYPE);

	private IJeiRuntime Runtime = null;

	@Override
	public void onRuntimeAvailable(@Nonnull IJeiRuntime jeiRuntime)
	{
		Runtime = jeiRuntime;
	}

	@Override
	public void registerCategories(@Nonnull IRecipeCategoryRegistration registration)
	{
		registration.addRecipeCategories(PART_CRAFTING_CATEGORY);
		registration.addRecipeCategories(GUN_CRAFTING_CATEGORY);
	}
	@Override
	public void registerRecipes(@Nonnull IRecipeRegistration registration)
	{
		ClientLevel world = Minecraft.getInstance().level;
		if (world != null)
		{
			registration.addRecipes(
				PART_FABRICATION_RECIPE_TYPE,
				world.getRecipeManager().getAllRecipesFor(FlansMod.PART_FABRICATION_RECIPE_TYPE.get()));
			registration.addRecipes(
				GUN_FABRICATION_RECIPE_TYPE,
				world.getRecipeManager().getAllRecipesFor(FlansMod.GUN_FABRICATION_RECIPE_TYPE.get()));
		}
	}
	@Override
	public void registerRecipeCatalysts(@Nonnull IRecipeCatalystRegistration registration)
	{
		for(ResourceLocation workbenchID : FlansMod.WORKBENCHES.getIds())
		{
			WorkbenchDefinition workbenchDefinition = FlansMod.WORKBENCHES.Get(workbenchID);
			Item workbenchItem = ForgeRegistries.ITEMS.getValue(workbenchID);
			if(workbenchItem != null && workbenchDefinition.IsValid())
			{
				if(workbenchDefinition.partCrafting.isActive)
					registration.addRecipeCatalyst(new ItemStack(workbenchItem), PART_FABRICATION_RECIPE_TYPE);
				if(workbenchDefinition.gunCrafting.isActive)
					registration.addRecipeCatalyst(new ItemStack(workbenchItem), GUN_FABRICATION_RECIPE_TYPE);
			}
		}
	}

	public void OpenPartFabList()
	{
		if(Runtime != null)
		{
			Runtime.getRecipesGui().showTypes(List.of(PART_FABRICATION_RECIPE_TYPE));
		}
	}
	public void OpenGunFabList()
	{
		if(Runtime != null)
		{
			Runtime.getRecipesGui().showTypes(List.of(GUN_FABRICATION_RECIPE_TYPE));
		}
	}
}