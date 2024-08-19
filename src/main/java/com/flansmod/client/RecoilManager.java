package com.flansmod.client;

import com.flansmod.util.Maths;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec2;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;

import javax.annotation.Nonnull;

public class RecoilManager
{
	private float RecoilPendingYaw = 0.0f;
	private float RecoilPendingPitch = 0.0f;

	private float RecoilStacksYaw = 0.0f;
	private float RecoilStacksPitch = 0.0f;
	private float RecoilStacksYawLast = 0.0f;
	private float RecoilStacksPitchLast = 0.0f;

	public float GetRecoilYaw(float dt) { return Maths.LerpF(RecoilStacksYawLast, RecoilStacksYaw, dt); }
	public float GetRecoilPitch(float dt) { return Maths.LerpF(RecoilStacksPitchLast, RecoilStacksPitch, dt); }

	public RecoilManager()
	{
		NeoForge.EVENT_BUS.register(this);
	}

	public void AddRecoil(float magYaw, float magPitch)
	{
		RecoilPendingYaw += magYaw;
		RecoilPendingPitch += magPitch;
	}

	public void OnPlayerLookInput(Vec2 delta)
	{

	}
	@SubscribeEvent
	public void OnCameraEvent(@Nonnull ViewportEvent.ComputeCameraAngles event)
	{
		event.setYaw(event.getYaw() + GetRecoilYaw(Minecraft.getInstance().getTimer().getGameTimeDeltaTicks()));
		event.setPitch(event.getPitch() - GetRecoilPitch(Minecraft.getInstance().getTimer().getGameTimeDeltaTicks()));
	}

	@SubscribeEvent
	public void OnRenderTick(@Nonnull RenderLevelStageEvent event)
	{

	}

	@SubscribeEvent
	public void OnClientTick(@Nonnull ClientTickEvent.Post event)
	{
		RecoilStacksYawLast = RecoilStacksYaw;
		RecoilStacksPitchLast = RecoilStacksPitch;

		float dYaw = RecoilPendingYaw * 0.5f;
		float dPitch = RecoilPendingPitch * 0.5f;
		//Player player = Minecraft.getInstance().player;
		//if (player != null)
		//{
		//	player.setXRot(player.getXRot() - dPitch);
		//	player.setYRot(player.getYRot() - dYaw);
		//}
		RecoilStacksYaw += dYaw;
		RecoilStacksPitch += dPitch;
		RecoilStacksYaw *= 0.75f;
		RecoilStacksPitch *= 0.75f;

		RecoilPendingYaw -= dYaw;
		RecoilPendingPitch -= dPitch;
	}
}
