package com.flansmod.client;

import com.flansmod.common.crafting.temporary.TemporaryWorkbench;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;

public class ClientInventoryManager
{
	private TemporaryWorkbench OpenWorkbench = null;

	@Nonnull
	public TemporaryWorkbench GetTemporaryInventory(@Nonnull ResourceLocation workbenchDef)
	{
		if(OpenWorkbench != null)
		{
			if(OpenWorkbench.Def.Location.equals(workbenchDef))
			{
				return OpenWorkbench;
			}
			else
			{
				OpenWorkbench.Close(Minecraft.getInstance().player);
			}
		}

		OpenWorkbench = new TemporaryWorkbench(workbenchDef);
		return OpenWorkbench;
	}

	public void CloseTemporaryInventory()
	{
		if(OpenWorkbench != null)
		{
			OpenWorkbench.Close(Minecraft.getInstance().player);
			OpenWorkbench = null;
		}
	}
}
