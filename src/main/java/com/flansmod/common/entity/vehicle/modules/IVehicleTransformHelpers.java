package com.flansmod.common.entity.vehicle.modules;

import com.flansmod.common.entity.ITransformPair;
import com.flansmod.common.entity.vehicle.hierarchy.VehicleComponentPath;
import com.flansmod.common.entity.vehicle.hierarchy.VehicleDefinitionHierarchy;
import com.flansmod.common.entity.vehicle.hierarchy.VehiclePartPath;
import com.flansmod.util.Maths;
import com.flansmod.util.Transform;
import com.flansmod.util.TransformStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface IVehicleTransformHelpers
{
	@Nonnull VehicleDefinitionHierarchy GetHierarchy();
	@Nonnull Transform GetRootTransformCurrent();
	@Nonnull Transform GetRootTransformPrevious();
	void SetRootTransformCurrent(@Nonnull Transform transform);
	void ApplyWorldToRootPrevious(@Nonnull TransformStack stack);
	void ApplyWorldToRootCurrent(@Nonnull TransformStack stack);
	//void ApplyPartToPartPrevious(@Nonnull VehicleDefinitionHierarchy.VehicleNode childPart, @Nonnull TransformStack stack);
	//void ApplyPartToPartCurrent(@Nonnull VehicleDefinitionHierarchy.VehicleNode childPart, @Nonnull TransformStack stack);


	void ApplyPartToPartPrevious(@Nonnull VehiclePartPath childPart, @Nonnull TransformStack stack);
	void ApplyPartToPartCurrent(@Nonnull VehiclePartPath childPart, @Nonnull TransformStack stack);
	void ApplyPartToComponentPrevious(@Nonnull VehicleComponentPath componentPath, @Nonnull TransformStack stack);
	void ApplyPartToComponentCurrent(@Nonnull VehicleComponentPath componentPath, @Nonnull TransformStack stack);





	default void Traverse(@Nonnull VehiclePartPath path, @Nonnull Consumer<VehicleDefinitionHierarchy.VehicleNode> func)
	{
		GetHierarchy().FromRootToNode(path, func);
	}

	// Root Transform settings
	default void SetPosition(double x, double y, double z) { SetRootTransformCurrent(GetRootTransformCurrent().WithPosition(x, y, z)); }
	default void SetPosition(@Nonnull Vec3 pos) { SetRootTransformCurrent(GetRootTransformCurrent().WithPosition(pos)); }
	default void SetYaw(float yaw) { SetRootTransformCurrent(GetRootTransformCurrent().WithYaw(yaw)); }
	default void SetPitch(float pitch) { SetRootTransformCurrent(GetRootTransformCurrent().WithPitch(pitch)); }
	default void SetRoll(float roll) { SetRootTransformCurrent(GetRootTransformCurrent().WithRoll(roll)); }
	default void RotateYaw(float yaw) { SetRootTransformCurrent(GetRootTransformCurrent().RotateYaw(yaw)); }
	default void RotatePitch(float pitch) { SetRootTransformCurrent(GetRootTransformCurrent().RotatePitch(pitch)); }
	default void RotateRoll(float roll) { SetRootTransformCurrent(GetRootTransformCurrent().RotateRoll(roll)); }
	default void SetEulerAngles(float pitch, float yaw, float roll) { SetRootTransformCurrent(GetRootTransformCurrent().WithEulerAngles(pitch, yaw, roll)); }



	@Nonnull default Transform GetWorldToRootPrevious() { return Transform.Flatten(this::ApplyWorldToRootPrevious); }
	@Nonnull default Transform GetWorldToRootCurrent() { return Transform.Flatten(this::ApplyWorldToRootCurrent); }
	@Nonnull default ITransformPair GetWorldToRoot() { return ITransformPair.of(this::GetWorldToRootPrevious, this::GetWorldToRootCurrent); }
	//@Nonnull default Transform GetPartToPartCurrent(@Nonnull VehicleDefinitionHierarchy.VehicleNode node) { return Transform.Flatten((stack) -> ApplyPartToPartPrevious(node, stack)); }
	//@Nonnull default Transform GetPartToPartPrevious(@Nonnull VehicleDefinitionHierarchy.VehicleNode node) { return Transform.Flatten((stack) -> ApplyPartToPartCurrent(node, stack)); }
	//@Nonnull default ITransformPair GetPartToPart(@Nonnull VehicleDefinitionHierarchy.VehicleNode node) { return ITransformPair.of(() -> GetPartToPartPrevious(node), () -> GetPartToPartCurrent(node)); }
	@Nonnull default Transform GetPartToPartPrevious(@Nonnull VehiclePartPath childPart) { return Transform.Flatten((stack) -> ApplyPartToPartPrevious(childPart, stack)); }
	@Nonnull default Transform GetPartToPartCurrent(@Nonnull VehiclePartPath childPart) { return Transform.Flatten((stack) -> ApplyPartToPartCurrent(childPart, stack)); }
	@Nonnull default Transform GetPartToComponentPrevious(@Nonnull VehicleComponentPath componentPath) { return Transform.Flatten((stack) -> ApplyPartToComponentPrevious(componentPath, stack)); }
	@Nonnull default Transform GetPartToComponentCurrent(@Nonnull VehicleComponentPath componentPath) { return Transform.Flatten((stack) -> ApplyPartToComponentCurrent(componentPath, stack)); }
	@Nonnull default ITransformPair GetPartToPart(@Nonnull VehiclePartPath childPart) { return ITransformPair.of(() -> GetPartToPartPrevious(childPart), () -> GetPartToPartCurrent(childPart)); }
	@Nonnull default ITransformPair GetPartToComponent(@Nonnull VehicleComponentPath componentPath) { return ITransformPair.of(() -> GetPartToComponentPrevious(componentPath), () -> GetPartToComponentCurrent(componentPath)); }



	// Root to Part
	default void TransformRootToPartPrevious(@Nonnull VehiclePartPath vehiclePart, @Nonnull TransformStack stack)
	{
		Traverse(vehiclePart, (node) -> { stack.add(GetPartToPartPrevious(vehiclePart)); });
	}
	@Nonnull default Transform GetRootToPartPrevious(@Nonnull VehiclePartPath vehiclePart)
	{
		return Transform.Flatten((stack) -> TransformRootToPartPrevious(vehiclePart, stack));
	}
	default void TransformRootToPartCurrent(@Nonnull VehiclePartPath vehiclePart, @Nonnull TransformStack stack)
	{
		Traverse(vehiclePart, (node) -> { stack.add(GetPartToPartCurrent(vehiclePart)); });
	}
	@Nonnull default Transform GetRootToPartCurrent(@Nonnull VehiclePartPath vehiclePart)
	{
		return Transform.Flatten((stack) -> TransformRootToPartCurrent(vehiclePart, stack));
	}
	@Nonnull default ITransformPair GetRootToPart(@Nonnull VehiclePartPath vehiclePart)
	{
		return ITransformPair.of(() -> GetRootToPartPrevious(vehiclePart), () -> GetRootToPartCurrent(vehiclePart));
	}


	// World to Part
	@Nonnull default ITransformPair GetWorldToPart(@Nonnull VehiclePartPath apPath) { return ITransformPair.compose(GetWorldToRoot(), GetRootToPart(apPath)); }
	@Nonnull default Transform GetWorldToPartPrevious(@Nonnull VehiclePartPath apPath) { return GetWorldToPart(apPath).GetPrevious(); }
	@Nonnull default Transform GetWorldToPartCurrent(@Nonnull VehiclePartPath apPath) { return GetWorldToPart(apPath).GetCurrent(); }
	@Nonnull default ITransformPair GetWorldToPart(@Nonnull VehicleComponentPath apPath) { return ITransformPair.compose(GetWorldToRoot(), GetRootToPart(apPath.Part())); }
	@Nonnull default Transform GetWorldToPartPrevious(@Nonnull VehicleComponentPath apPath) { return GetWorldToPart(apPath.Part()).GetPrevious(); }
	@Nonnull default Transform GetWorldToPartCurrent(@Nonnull VehicleComponentPath apPath) { return GetWorldToPart(apPath.Part()).GetCurrent(); }



	// ---------------------------------------------------------------------------------------------------------
	// Raycasts
	// Start and end should be relative to the root node
	default void Raycast(@Nonnull Vec3 start,
						 @Nonnull Vec3 end,
						 float dt,
						 @Nonnull BiConsumer<VehiclePartPath, Vec3> func)
	{
		if(GetHierarchy().RootNode != null)
		{
			TransformStack stack = TransformStack.of(GetWorldToRoot().GetDelta(dt));
			Raycast(stack, GetHierarchy().RootNode, start, end, dt, func);
		}
	}
	default void Raycast(@Nonnull TransformStack stack,
						 @Nonnull VehicleDefinitionHierarchy.VehicleNode node,
						 @Nonnull Vec3 start,
						 @Nonnull Vec3 end,
						 float dt,
						 @Nonnull BiConsumer<VehiclePartPath, Vec3> func)
	{
		stack.PushSaveState();

		// If this piece is articulated, add a transform
		if(node.Def.IsArticulated())
		{
			Transform articulation = GetPartToPart(node.GetPath()).GetDelta(dt);
			if(!articulation.IsIdentity())
			{
				start = articulation.GlobalToLocalPosition(start);
				end = articulation.GlobalToLocalPosition(end);
				stack.add(articulation);
			}
		}

		// Cast against children nodes
		for(VehicleDefinitionHierarchy.VehicleNode child : node.ChildNodes.values())
		{
			Raycast(stack, child, start, end, dt, func);
		}

		// If this piece has a hitbox (needs damageable), cast against it
		if(node.Def.IsDamageable())
		{
			stack.PushSaveState();
			stack.add(Transform.FromPos(node.Def.damage.hitboxCenter));
			Vector3d hitPos = new Vector3d();
			if(Maths.RayBoxIntersect(start, end, stack.Top(), node.Def.damage.hitboxHalfExtents.toVector3f(), hitPos))
			{
				func.accept(node.GetPath(), new Vec3(hitPos.x, hitPos.y, hitPos.z));
			}
			stack.PopSaveState();
		}

		stack.PopSaveState();
	}
}
