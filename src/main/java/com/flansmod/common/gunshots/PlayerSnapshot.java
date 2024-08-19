package com.flansmod.common.gunshots;

import com.flansmod.common.FlansMod;
import com.flansmod.util.Maths;
import com.flansmod.util.Transform;
import com.flansmod.util.TransformStack;
import com.mojang.math.Transformation;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector4f;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class PlayerSnapshot
{
    public static final PlayerSnapshot INVALID = new PlayerSnapshot();

    public boolean valid;
    public final PlayerHitbox[] hitboxes;

    public long time;

    public PlayerSnapshot()
    {
        valid = false;
        hitboxes = new PlayerHitbox[EPlayerHitArea.NUM_AREAS];
        for(int i = 0; i < EPlayerHitArea.NUM_AREAS; i++)
        {
            hitboxes[i] = new PlayerHitbox(EPlayerHitArea.values()[i], Transform.IDENTITY, Maths.IdentityPosF());
        }
    }

    public void UpdateHitbox(@Nonnull EPlayerHitArea area, @Nonnull Transform centerPoint, @Nonnull Vector3f halfExtents)
    {
        hitboxes[area.ordinal()].transform = centerPoint;
        hitboxes[area.ordinal()].halfExtents = halfExtents;
    }

    public void Raycast(@Nonnull Player player, @Nonnull Vec3 startPos, @Nonnull Vec3 endPos, @Nonnull List<HitResult> results)
    {
        for(PlayerHitbox hitbox : hitboxes)
        {
            Vector3d hitLoc = new Vector3d();
            if(hitbox.Raycast(startPos, endPos, hitLoc))
            {
                results.add(new PlayerHitResult(player, hitbox, new Vec3(hitLoc.x, hitLoc.y, hitLoc.z)));
            }
        }
    }

    @Nonnull
    public Transform GetArmTransform(@Nonnull HumanoidArm arm)
    {
        return switch(arm) {
            case RIGHT -> hitboxes[EPlayerHitArea.RIGHTARM.ordinal()].transform;
            case LEFT -> hitboxes[EPlayerHitArea.LEFTARM.ordinal()].transform;
        };
    }

    // ----------------------------------------------------------
    // Debug Rendering
    @OnlyIn(Dist.CLIENT)
    public void debugRender(@Nonnull Vec3 withOffset, @Nonnull Vector4f colour)
    {
        for(PlayerHitbox hitbox : hitboxes)
            if(hitbox != null)
                hitbox.debugRender(withOffset, colour);
    }


}
