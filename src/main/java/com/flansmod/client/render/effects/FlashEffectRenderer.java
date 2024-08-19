package com.flansmod.client.render.effects;

import com.flansmod.client.render.FirstPersonManager;
import com.flansmod.client.render.RenderContext;
import com.flansmod.client.render.models.FlansModelRegistry;
import com.flansmod.client.render.models.ITurboRenderer;
import com.flansmod.common.actions.ActionGroupInstance;
import com.flansmod.common.actions.ActionInstance;
import com.flansmod.common.actions.contexts.GunContext;
import com.flansmod.common.actions.contexts.GunContextPlayer;
import com.flansmod.common.actions.contexts.ShooterContext;
import com.flansmod.common.actions.nodes.AttachEffectAction;
import com.flansmod.util.MinecraftHelpers;
import com.flansmod.util.Transform;
import net.neoforged.bus.api.SubscribeEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class FlashEffectRenderer
{
	public FlashEffectRenderer()
	{
		NeoForge.EVENT_BUS.register(this);
	}

	@SubscribeEvent
	public void OnRenderFirstPersonHands(@Nonnull RenderHandEvent event)
	{
		if (Minecraft.getInstance().level == null)
			return;
		ShooterContext playerContext = ShooterContext.of(Minecraft.getInstance().player);
		if (!playerContext.IsValid())
			return;

		for (GunContext gunContext : playerContext.GetAllGunContexts(true))
		{
			if (!gunContext.IsValid())
				continue;

			for (ActionGroupInstance actionGroup : gunContext.GetActionStack().GetActiveActionGroups())
			{

				for (ActionInstance actionInstance : actionGroup.GetActions())
				{
					if (actionInstance instanceof AttachEffectAction attachEffectAction)
					{
						if(attachEffectAction.TicksSinceTrigger > 0)
							continue;

						RenderFlashEffectOnEntity(event.getPoseStack(),
							null,
							gunContext,
							attachEffectAction.GetRelativeAPName(),
							attachEffectAction.EffectModelLocation(),
							attachEffectAction.EffectTextureLocation());
					}
				}
			}
		}
	}


	@SubscribeEvent
	public void RenderTick(@Nonnull RenderLevelStageEvent event)
	{
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES)
			return;
		if (Minecraft.getInstance().level == null)
			return;

		RenderSystem.setShader(GameRenderer::getPositionTexColorShader);

		for(Player player : Minecraft.getInstance().level.players())
		{
			ShooterContext playerContext = ShooterContext.of(player);
			if (!playerContext.IsValid())
				continue;

			// We actually don't want to render the player effect in worldspace
			if(playerContext.IsLocalPlayerOwner() && Minecraft.getInstance().options.getCameraType().isFirstPerson())
				continue;

			for (GunContext gunContext : playerContext.GetAllGunContexts(true))
			{
				if (!gunContext.IsValid())
					continue;

				for (ActionGroupInstance actionGroup : gunContext.GetActionStack().GetActiveActionGroups())
				{

					for (ActionInstance actionInstance : actionGroup.GetActions())
					{
						if (actionInstance instanceof AttachEffectAction attachEffectAction)
						{
							if(attachEffectAction.TicksSinceTrigger > 0)
								continue;

							RenderFlashEffectOnEntity(event.getPoseStack(),
								event.getCamera(),
								gunContext,
								attachEffectAction.GetRelativeAPName(),
								attachEffectAction.EffectModelLocation(),
								attachEffectAction.EffectTextureLocation());
						}
					}
				}
			}
		}
	}

	private final RandomSource random = RandomSource.create();
	public void RenderFlashEffectOnEntity(@Nonnull PoseStack poseStack,
										  @Nullable Camera camera,
										  @Nonnull GunContext gunContext,
										  @Nonnull String relativeAPName,
										  @Nonnull ResourceLocation modelLoc,
										  @Nonnull ResourceLocation textureLoc)
	{
		ITurboRenderer renderer = FlansModelRegistry.GetItemRenderer(modelLoc);
		if(renderer != null)
		{
			ItemDisplayContext transformType = ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
			if (gunContext instanceof GunContextPlayer playerGunContext)
			{
				if (!Minecraft.getInstance().options.getCameraType().isFirstPerson() || !gunContext.GetShooter().IsLocalPlayerOwner())
				{
					transformType = MinecraftHelpers.GetThirdPersonTransformType(gunContext.GetShooter().IsLocalPlayerOwner(), playerGunContext.GetHand());
				} else
				{
					transformType = MinecraftHelpers.GetFirstPersonTransformType(playerGunContext.GetHand());
				}
			}

			int light = camera != null ? camera.getBlockAtCamera().getLightEmission() : 0;

			RenderContext renderContext = new RenderContext(
				Minecraft.getInstance().renderBuffers().bufferSource(),
				transformType,
				poseStack,
				light,
				0);

			renderContext.Transforms.PushSaveState();

			if(camera != null)
				renderContext.Transforms.add(Transform.FromPos(camera.getPosition().scale(-1d)));

			if(transformType.firstPerson())
			{
				FirstPersonManager.ApplyEyeToRoot(renderContext.Transforms, gunContext, transformType);
				FirstPersonManager.ApplyRootToModel(renderContext.Transforms, gunContext, transformType);
				FirstPersonManager.ApplyModelToAP(renderContext.Transforms, gunContext, relativeAPName, true);
				renderContext.Transforms.add(Transform.FromScale(2.0f));
			}
			else
			{
				renderContext.Transforms.add(
					FirstPersonManager.GetWorldSpaceAPTransform(gunContext, transformType, relativeAPName));
			}


			renderContext.Transforms.add(Transform.FromPos(0d, 0d, -1d/16d));
			renderer.RenderDirect(null, null, renderContext);
			renderContext.Transforms.PopSaveState();
		}
	}
}
