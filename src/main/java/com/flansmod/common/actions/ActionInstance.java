package com.flansmod.common.actions;

import com.flansmod.common.actions.contexts.GunContext;
import com.flansmod.common.gunshots.*;
import com.flansmod.common.types.Constants;
import com.flansmod.common.types.guns.elements.ActionDefinition;
import com.flansmod.common.types.elements.ModifierDefinition;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import java.util.List;

public abstract class ActionInstance
{
	public static final float TICK_RATE = 1.0f / 20.0f;
	@Nonnull
	public final ActionGroupInstance Group;
	@Nonnull
	public final ActionDefinition Def;

	public static abstract class NetData
	{
		public abstract int GetID();
		public abstract void Encode(FriendlyByteBuf buf);
		public abstract void Decode(FriendlyByteBuf buf);

		public static final int INVALID_ID = -1;
		public static final NetData Invalid = new NetData()
		{
			@Override
			public int GetID() { return INVALID_ID; }
			@Override
			public void Encode(FriendlyByteBuf buf) {}
			@Override
			public void Decode(FriendlyByteBuf buf) {}
		};
	}

	public boolean Finished() { return Group.Finished; }
	public float GetCharge() { return Group.Charge; }
	public boolean IsCharging() { return Group.IsCharging; }
	public int GetProgressTicks() { return Group.Progress; }
	public float GetProgressSeconds() { return Group.Progress * TICK_RATE; }

	public int GetDurationPerTriggerTicks() { return Group.Duration; }
	public float GetDurationPerTriggerSeconds() { return Group.Duration * TICK_RATE; }

	public int GetDurationTotalTicks() { return Group.Duration * Group.TriggerCount; }
	public float GetDurationTotalSeconds() { return Group.Duration * Group.TriggerCount * TICK_RATE; }
	public int GetTriggerCount() { return Group.TriggerCount; }

	public int GetNumBurstsRemaining() { return Group.NumBurstsRemaining; }

	public ActionInstance(@Nonnull ActionGroupInstance group, @Nonnull ActionDefinition def)
	{
		Group = group;
		Def = def;
	}

	public boolean ShouldRender(GunContext context) { return true; }
	public boolean PropogateToServer() { return true; }
	public boolean ShouldFallBackToReload() { return false; }
	public EActionResult CanStart() { return EActionResult.CanProcess; }
	public boolean CanRetrigger() { return true; }
	public abstract void OnTriggerClient(int triggerIndex);
	public abstract void OnTriggerServer(int triggerIndex);
	public void OnStartServer() {}
	public void OnTickServer() {}
	public void OnFinishServer() {}
	public void OnStartClient() {}
	public void OnTickClient() {}
	public void OnFinishClient() {}
	public void SkipTicks(int ticks) {}

	// NetData and sync
	// Careful when changing this. The action group will only propogate to players within range of the furthest action in the group
	public double GetPropogationRadius() { return 200.0d; }
	public void AddExtraPositionsForNetSync(int triggerIndex, List<Vec3> positions) {}
	public boolean ShouldNetSyncAroundPlayer() { return true; }
	@Nonnull
	public NetData GetNetDataForTrigger(int triggerIndex)
	{
		return NetData.Invalid;
	}
	public void UpdateFromNetData(NetData netData, int triggerIndex) {}

	public boolean VerifyServer(GunshotCollection shots) { return true; }



	// These ones are specific to this action
	public float GetFloat(@Nonnull String stat) { return Group.Context.ModifyFloat(stat).get(); }
	public float ModifyFloat(@Nonnull String stat, float defaultValue) { return Group.Context.ModifyFloat(stat).apply(defaultValue); }
	@Nonnull
	public String ModifyString(@Nonnull String stat, @Nonnull String defaultValue) { return Group.Context.ModifyString(stat, defaultValue); }

	public float Duration() { return ModifyFloat(Constants.STAT_DURATION, Def.duration); }
	public float ToolLevel() { return GetFloat(Constants.STAT_TOOL_HARVEST_LEVEL); }
	public float HarvestSpeed() { return GetFloat(Constants.STAT_TOOL_HARVEST_SPEED); }
	public float Reach() { return GetFloat(Constants.STAT_TOOL_REACH); }
}
