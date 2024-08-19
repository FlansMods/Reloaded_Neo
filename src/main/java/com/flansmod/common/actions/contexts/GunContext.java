package com.flansmod.common.actions.contexts;

import com.flansmod.client.FlansModClient;
import com.flansmod.common.FlansMod;
import com.flansmod.common.abilities.AbilityInstanceApplyModifier;
import com.flansmod.common.abilities.AbilityStack;
import com.flansmod.common.abilities.IAbilityEffect;
import com.flansmod.common.actions.*;
import com.flansmod.common.actions.stats.*;
import com.flansmod.common.item.*;
import com.flansmod.common.types.abilities.CraftingTraitDefinition;
import com.flansmod.common.types.abilities.elements.AbilityEffectDefinition;
import com.flansmod.common.types.abilities.elements.EAbilityEffect;
import com.flansmod.common.types.abilities.elements.EAbilityTrigger;
import com.flansmod.common.types.attachments.AttachmentDefinition;
import com.flansmod.common.types.attachments.EAttachmentType;
import com.flansmod.common.types.guns.elements.*;
import com.flansmod.common.types.elements.ModifierDefinition;
import com.flansmod.common.types.guns.*;
import com.flansmod.common.types.parts.PartDefinition;
import com.flansmod.common.types.elements.EPlayerInput;
import com.flansmod.util.MinecraftHelpers;
import com.flansmod.util.Transform;
import com.flansmod.util.formulae.FloatAccumulation;
import com.mojang.datafixers.util.Pair;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.LegacyRandomSource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiConsumer;

public abstract class GunContext implements IStatCalculatorContext
{
	public static final GunContext INVALID = new GunContext(ItemStack.EMPTY)
	{
		@Override
		public void OnItemStackChanged(@Nonnull ItemStack stack) {}
		@Override
		@Nonnull
		public EItemStackLinkage CheckItemStackLink() { return EItemStackLinkage.NotConnected; }
		@Override
		@Nonnull
		public ItemStack GetLinkedItemStack() { return ItemStack.EMPTY; }
		@Override
		@Nullable
		public DamageSource CreateDamageSource() { return null; }
		@Override
		@Nonnull
		public ShooterContext GetShooter() { return ShooterContext.INVALID; }
		@Override
		@Nullable
		public Inventory GetAttachedInventory() { return null; }
		@Override
		public boolean CanPerformTwoHandedAction() { return false; }
		@Override
		@Nonnull
		public ActionStack GetActionStack() { return ActionStack.Invalid; }
		@Override
		public boolean CanPerformActions() { return false; }
		@Override
		public void BakeModifiers(@Nonnull IModifierBaker baker) { }
	};

	// -----------------------------------------------------------------------------------------------
	// A bunch of "of" methods to let you really easily just grab a context wherever you need it
	// -----------------------------------------------------------------------------------------------
	@Nonnull
	public static GunContext of(@Nonnull ItemStack stack)
	{
		return unknownSide(stack);
	}
	@Nonnull
	public static GunContext of(@Nonnull ItemStack stack, EContextSide side) {
		return switch(side) {
			case Client -> client(stack);
			case Server -> server(stack);
			default -> unknownSide(stack);
		};
	}
	@Nonnull
	public static GunContext of(@Nonnull ShooterContext shooter, @Nonnull UUID gunID)
	{
		switch(shooter.GetSide())
		{
			case Client -> { return client(shooter, gunID); }
			case Server -> { return server(shooter, gunID); }
			default -> { return unknownSide(gunID); }
		}
	}
	@Nonnull
	public static GunContext of(@Nonnull ShooterContext shooter, int index)
	{
		switch(shooter.GetSide())
		{
			case Client -> { return client(shooter, index); }
			case Server -> { return server(shooter, index); }
			default -> { return unknownSide(shooter.GetGunIDForSlot(index)); }
		}
	}
	@Nonnull
	public static GunContext of(@Nonnull ShooterContext shooter, @Nonnull InteractionHand hand)
	{
		if(shooter instanceof ShooterContextLiving livingContext)
		{
			switch(shooter.GetSide())
			{
				case Client -> { return client(livingContext, hand); }
				case Server -> { return server(livingContext, hand); }
				default -> { return unknownSide(livingContext.GetGunID(hand)); }
			}
		}
		return GunContext.INVALID;
	}
	@Nonnull
	public static GunContext of(@Nonnull UUID gunID)
	{
		switch(MinecraftHelpers.GetLogicalSide())
		{
			case Client -> { return client(gunID); }
			case Server -> { return server(gunID); }
			default -> { return unknownSide(gunID); }
		}
	}
	@Nonnull
	public static GunContext of(@Nonnull ItemEntity itemEntity)
	{
		return itemEntity.level().isClientSide ? client(itemEntity) : server(itemEntity);
	}
	@Nonnull
	public static GunContext of(@Nonnull Container container, int slotIndex, boolean isClient)
	{
		return isClient ? client(container, slotIndex) : server(container, slotIndex);
	}
	@Nonnull
	public static GunContext of(@Nonnull BlockEntity blockEntity, @Nonnull Container container, int slotIndex)
	{
		switch(EContextSide.of(blockEntity))
		{
			case Client -> { return client(blockEntity, container, slotIndex); }
			case Server -> { return server(blockEntity, container, slotIndex); }
			default -> { return unknownSide(blockEntity, container, slotIndex); }
		}
	}



	// -----------------------------------------------------------------------------------------------------------------
	// Client specific - keeping these private for now, could go public if we trust general code to get it right, hmm..
	// -----------------------------------------------------------------------------------------------------------------
	@Nonnull
	private static GunContext client(@Nonnull UUID gunID) { return FlansModClient.CONTEXT_CACHE.GetLastKnownAppearanceOfGun(gunID); }
	@Nonnull
	private static GunContext client(@Nonnull ItemStack stack) { return FlansModClient.CONTEXT_CACHE.Create(stack); }
	@Nonnull
	private static GunContext client(@Nonnull Container container, int slotIndex) { return FlansModClient.CONTEXT_CACHE.Create(container, slotIndex); }
	@Nonnull
	private static GunContext client(@Nonnull BlockEntity blockEntity, @Nonnull Container container, int slotIndex) { return FlansModClient.CONTEXT_CACHE.Create(blockEntity, container, slotIndex); }
	@Nonnull
	private static GunContext client(@Nonnull ShooterContext shooter, int slotIndex) { return FlansModClient.CONTEXT_CACHE.Create(shooter, slotIndex);  }
	@Nonnull
	private static GunContext client(@Nonnull ShooterContext shooter, @Nonnull UUID gunID) { return FlansModClient.CONTEXT_CACHE.Create(shooter, gunID); }
	@Nonnull
	private static GunContext client(@Nonnull ShooterContextLiving living, @Nonnull InteractionHand hand) { return FlansModClient.CONTEXT_CACHE.Create(living, hand); }
	@Nonnull
	private static GunContext client(@Nonnull ItemEntity itemEntity) { return FlansModClient.CONTEXT_CACHE.Create(itemEntity); }

	// -----------------------------------------------------------------------------------------------------------------
	// Server specific - same applies
	// -----------------------------------------------------------------------------------------------------------------
	@Nonnull
	private static GunContext server(@Nonnull UUID gunID) { return FlansMod.CONTEXT_CACHE.GetLastKnownAppearanceOfGun(gunID); }
	@Nonnull
	private static GunContext server(@Nonnull ItemStack stack) { return FlansMod.CONTEXT_CACHE.Create(stack); }
	@Nonnull
	private static GunContext server(@Nonnull Container container, int slotIndex) { return FlansMod.CONTEXT_CACHE.Create(container, slotIndex); }
	@Nonnull
	private static GunContext server(@Nonnull BlockEntity blockEntity, @Nonnull Container container, int slotIndex) { return FlansMod.CONTEXT_CACHE.Create(blockEntity, container, slotIndex); }
	@Nonnull
	private static GunContext server(@Nonnull ShooterContext shooter, int slotIndex) { return FlansMod.CONTEXT_CACHE.Create(shooter, slotIndex);  }
	@Nonnull
	private static GunContext server(@Nonnull ShooterContext shooter, @Nonnull UUID gunID) { return FlansMod.CONTEXT_CACHE.Create(shooter, gunID); }
	@Nonnull
	private static GunContext server(@Nonnull ShooterContextLiving living, @Nonnull InteractionHand hand) { return FlansMod.CONTEXT_CACHE.Create(living, hand); }
	@Nonnull
	private static GunContext server(@Nonnull ItemEntity itemEntity) { return FlansMod.CONTEXT_CACHE.Create(itemEntity); }

	// -----------------------------------------------------------------------------------------------------------------
	// Side unknown, these are generally low quality contexts, so should only be used in limited situations,
	// like rendering. You won't get ActionStacks or inventory management here.
	// -----------------------------------------------------------------------------------------------------------------
	@Nonnull
	private static GunContext unknownSide(@Nonnull BlockEntity blockEntity, @Nonnull Container container, int slotIndex) { return ContextCache.CreateWithoutCaching(container.getItem(slotIndex)); }
	@Nonnull
	private static GunContext unknownSide(@Nonnull ItemStack stack) { return ContextCache.CreateWithoutCaching(stack); }
	// TODO: Is this a valid query?
	@Nonnull
	private static GunContext unknownSide(@Nonnull UUID gunID) { return INVALID; }

	// ---------------------------------------------------------------------------------------------------
	// CONTEXT CACHES (Contained in the GunContext and built over time)
	// ---------------------------------------------------------------------------------------------------
	private final HashMap<EPlayerInput, GunInputContext> InputContextCache = new HashMap<>();
	private final HashMap<String, ActionGroupContext> ActionGroupContextCache = new HashMap<>();
	private final HashMap<Integer, String> CachedGroupPathNames = new HashMap<>();
	@Nonnull
	public GunInputContext GetInputContext(EPlayerInput inputType)
	{
		if(InputContextCache.containsKey(inputType))
			return InputContextCache.get(inputType);

		GunInputContext context = new GunInputContext(this, inputType);
		InputContextCache.put(inputType, context);
		return context;
	}
	@Nonnull
	public ActionGroupContext GetActionGroupContext(String groupPath)
	{
		if(groupPath == null)
			return ActionGroupContext.INVALID;

		if(ActionGroupContextCache.containsKey(groupPath))
			return ActionGroupContextCache.get(groupPath);

		ActionGroupContext context = new ActionGroupContext(this, groupPath);
		ActionGroupContextCache.put(groupPath, context);
		return context;
	}
	@Nonnull
	public ActionGroupContext GetActionGroupContextByHash(int groupPathHash)
	{
		if(!CachedGroupPathNames.containsKey(groupPathHash))
		{
			for(ActionGroupDefinition agDef : Def.actionGroups)
			{
				String groupPath = ActionGroupContext.CreateGroupPath(agDef.key);
				CachedGroupPathNames.put(groupPath.hashCode(), groupPath);
			}
			for(EAttachmentType attachmentType : EAttachmentType.values())
			{
				for (int i = 0; i < GetNumAttachmentStacks(attachmentType); i++)
				{
					AttachmentDefinition attachmentDefintion = GetAttachmentDefinition(attachmentType, i);
					if(attachmentDefintion.IsValid())
					{
						for(ActionGroupDefinition agDef : attachmentDefintion.actionOverrides)
						{
							String groupPath = ActionGroupContext.CreateGroupPath(attachmentType, i, agDef.key);
							CachedGroupPathNames.put(groupPath.hashCode(), groupPath);
						}
					}
				}
			}
		}
		return GetActionGroupContext(CachedGroupPathNames.get(groupPathHash));
	}
	@Nonnull
	public ActionGroupContext GetActionGroupContextSibling(ActionGroupContext original, String newKey)
	{
		if(original.IsAttachment())
		{
			ActionGroupContext.CreateGroupPath(original.GetAttachmentType(), original.GetAttachmentIndex(), newKey);
		}
		return GetActionGroupContext(newKey);
	}

	public ItemStack Stack;
	@Nonnull
	public final GunDefinition Def;

	// --------------------------------------------------------------------------
	// Abstractions
	// --------------------------------------------------------------------------
	public enum EItemStackLinkage
	{
		NotConnected,
		LostConnection,
		Connected,
	}

	public enum EItemStackValidity
	{
		Invalid_Error,
		Invalid_DifferentItem,
		Invalid_GunIDChange,
		Valid_NoChanges,
		Valid_TagChanges;

		public boolean IsValid() { return this == Valid_NoChanges || this == Valid_TagChanges; }
	}

	public abstract void OnItemStackChanged(ItemStack stack);
	@Nonnull
	public abstract EItemStackLinkage CheckItemStackLink();
	@Nonnull
	public abstract ItemStack GetLinkedItemStack();
	public abstract DamageSource CreateDamageSource();
	@Nonnull
	public abstract ShooterContext GetShooter();
	public abstract Container GetAttachedInventory();
	public abstract boolean CanPerformTwoHandedAction();
	// Not necessarily valid to ask for a hand, but in cases where it is valid, use this
	public int GetInventorySlotIndex() { return Inventory.NOT_FOUND_INDEX; }
	public abstract void BakeModifiers(@Nonnull IModifierBaker baker);
	@Nonnull
	public abstract ActionStack GetActionStack();
	public abstract boolean CanPerformActions();
	@Nullable
	public Level GetLevel() { return null; }
	@Nullable
	public Transform GetPosition() { return null; }

	// --------------------------------------------------------------------------
	// Helpers
	// --------------------------------------------------------------------------
	@Nonnull
	public ItemStack GetItemStack() { return Stack; }
	public void SetItemStack(@Nonnull ItemStack stack)
	{
		Stack = stack;
		OnItemStackChanged(stack);
	}
	@Nonnull
	public Transform GetShootOrigin() { return GetShooter().GetShootOrigin(0f); }
	@Nonnull
	public Transform GetShootOrigin(float deltaTick) { return GetShooter().GetShootOrigin(deltaTick); }
	private static final RandomSource BackupRandomGenerator = new LegacyRandomSource(8931145513320L);
	@Nonnull
	public RandomSource GetRandom()
	{
		Level level = GetLevel();
		return level != null ? level.random : BackupRandomGenerator;
	}

	// Return: Any changes?
	public boolean IsLinkedToItemStack()
	{
		return CheckItemStackLink() == EItemStackLinkage.Connected;
	}
	@Nonnull
	public static EItemStackValidity CompareGunStacks(@Nonnull ItemStack a, @Nonnull ItemStack b)
	{
		if(a.getItem() != b.getItem())
			return EItemStackValidity.Invalid_DifferentItem;
		if(!FlanItem.GetGunID(a).equals(FlanItem.GetGunID(b)))
			return EItemStackValidity.Invalid_GunIDChange;
		if(!ItemStack.isSameItemSameTags(a, b))
			return EItemStackValidity.Valid_TagChanges;
		return EItemStackValidity.Valid_NoChanges;
	}
	@Nonnull
	public EItemStackValidity ValidateLinkedItemStack()
	{
		if(IsLinkedToItemStack())
		{
			return CompareGunStacks(GetLinkedItemStack(), Stack);
		}
		return EItemStackValidity.Invalid_Error;
	}
	public void UpdateFromItemStack()
	{
		if(IsLinkedToItemStack())
		{
			EItemStackValidity validity = ValidateLinkedItemStack();
			if(validity.IsValid())
			{
				if(!ItemStack.isSameItemSameTags(Stack, GetLinkedItemStack()))
					Stack = GetLinkedItemStack().copy();
			}
		}
	}
	public boolean IsValid()
	{
		if(IsLinkedToItemStack())
		{
			if (!ValidateLinkedItemStack().IsValid())
				return false;
		}

		if(Stack.isEmpty())
			return false;
		return Def.IsValid();
	}
	@Nonnull
	public UUID GetUUID()
	{
		return FlanItem.GetGunID(Stack);
	}
	protected GunContext(@Nonnull ItemStack stackAtTimeOfCreation)
	{
		Stack = stackAtTimeOfCreation.copy();
		Def = CacheGunDefinition();
		ModCache = new ModifierCache(this::BakeAllModifiers);
	}
	/// Public accessors of accumulated stats
	// Make sure we process attachments when getting stats and results about the gun
	@Nonnull
	public GunDefinition CacheGunDefinition()
	{
		if(GetItemStack().getItem() instanceof GunItem gunItem)
			return gunItem.Def();
		return GunDefinition.INVALID;
	}

	// --------------------------------------------------------------------------
	// ItemStack Operations
	// --------------------------------------------------------------------------
	@Nonnull
	protected CompoundTag GetTags(@Nonnull String key)
	{
		CompoundTag root = GetItemStack().getOrCreateTag();
		if(root.contains(key))
			return root.getCompound(key);
		return new CompoundTag();
	}
	protected void SetTags(@Nonnull String key, @Nonnull CompoundTag tags)
	{
		ItemStack updatedStack = Stack.copy();
		updatedStack.getOrCreateTag().put(key, tags);
		SetItemStack(updatedStack);
	}
	// --------------------------------------------------------------------------
	// CRAFTING HISTORY
	// --------------------------------------------------------------------------
	// Only remember parts that we used, not arbitrary item stacks with NBT
	public PartDefinition[] GetCraftingInputs()
	{
		return FlanItem.GetCraftingInputs(GetItemStack());
	}

	public void SetCraftingInputs(ItemStack[] stacks)
	{
		ItemStack gunStack = GetItemStack();
		FlanItem.SetCraftingInputs(gunStack, stacks);
		SetItemStack(gunStack);
	}

	// --------------------------------------------------------------------------
	// DEFINITION PEEKING
	// --------------------------------------------------------------------------
	@Nonnull
	public List<ReloadDefinition> GetReloadDefintions()
	{
		List<ReloadDefinition> results = new ArrayList<>(Arrays.asList(Def.reloads));
		for(AttachmentDefinition attachmentDef : GetAttachmentDefinitions())
		{
			results.addAll(Arrays.asList(attachmentDef.reloadOverrides));
		}
		return results;
	}
	@Nonnull
	private ReloadDefinition[] GetReloadDefinitionsForSubpath(ActionGroupContext groupContext)
	{
		if(groupContext.IsAttachment())
			return GetAttachmentDefinition(groupContext.GetAttachmentType(), groupContext.GetAttachmentIndex()).reloadOverrides;
		return Def.reloads;
	}
	@Nullable
	public ReloadDefinition GetReloadDefinitionContaining(ActionGroupContext groupContext)
	{
		for(ReloadDefinition reload : GetReloadDefinitionsForSubpath(groupContext))
		{
			if(reload.Contains(groupContext.GroupPath))
				return reload;
		}
		return null;
	}
	@Nonnull
	public List<ActionDefinition> GetPotentialPrimaryActions()
	{
		return GetPotentialActions(EPlayerInput.Fire1);
	}
	@Nonnull
	public List<ActionDefinition> GetPotentialSecondaryActions()
	{
		return GetPotentialActions(EPlayerInput.Fire2);
	}
	@Nonnull
	public List<ActionDefinition> GetPotentialActions(EPlayerInput inputType)
	{
		List<ActionDefinition> results = new ArrayList<>();
		GunInputContext inputContext = GetInputContext(inputType);
		for(var kvp : EvaluateInputHandler(inputContext))
		{
			results.addAll(Arrays.asList(kvp.getFirst().Def.actions));
		}
		return results;
	}

	// --------------------------------------------------------------------------
	// INPUTS
	// --------------------------------------------------------------------------
	public List<Pair<ActionGroupContext, Boolean>> EvaluateInputHandler(GunInputContext inputContext)
	{
		List<Pair<ActionGroupContext, Boolean>> results = new ArrayList<>();
		HandlerDefinition handler = Def.GetInputHandler(inputContext);
		for(HandlerNodeDefinition node : handler.nodes)
		{
			if(!node.modalCheck.isEmpty())
			{
				String modeKey = node.modalCheck;
				String modeValue = "on";
				if(node.modalCheck.contains(":"))
				{
					String[] split = node.modalCheck.split(":");
					modeKey = split[0];
					modeValue = split[1];
				}

				String currentValue = inputContext.Gun.GetModeValue(modeKey);
				if(!currentValue.equals(modeValue))
					continue;
			}
			if(!node.canTriggerWhileReloading)
			{
				if(inputContext.Gun.GetActionStack().IsReloading())
					continue;
			}
			if(node.deferToAttachment)
			{
				AttachmentDefinition attachmentDef = GetAttachmentDefinition(node.attachmentType, node.attachmentIndex);
				if(attachmentDef.IsValid())
				{
					EvaluateAttachmentInputHandler(inputContext,
						ActionGroupContext.CreateGroupPath(node.attachmentType, node.attachmentIndex, ""),
						attachmentDef,
						results);
				}
			}
			else
			{
				ActionGroupDefinition actionGroupDef = Def.GetActionGroup(node.actionGroupToTrigger);
				if(actionGroupDef.IsValid())
				{
					results.add(Pair.of(ActionGroupContext.CreateFrom(inputContext.Gun, node.actionGroupToTrigger), node.andContinueEvaluating));
				}
			}
		}
		return results;
	}
	public void EvaluateAttachmentInputHandler(GunInputContext inputContext, String prefix, AttachmentDefinition attachmentDef, List<Pair<ActionGroupContext, Boolean>> results)
	{
		HandlerDefinition attachmentInputHandler = attachmentDef.GetInputHandler(inputContext);
		for(HandlerNodeDefinition node : attachmentInputHandler.nodes)
		{
			boolean matched = false;
			if(!node.modalCheck.isEmpty())
			{
				String modeKey = node.modalCheck;
				String modeValue = "on";
				if(node.modalCheck.contains(":"))
				{
					String[] split = node.modalCheck.split(":");
					modeKey = split[0];
					modeValue = split[1];
				}

				String currentValue = inputContext.Gun.GetModeValue(modeKey);
				if(!currentValue.equals(modeValue))
					continue;
			}

			if(node.deferToAttachment)
			{
				FlansMod.LOGGER.warn(inputContext.Gun + ": Attachment is deferring to attachment in handlerOverrides");
			}
			else
			{
				ActionGroupDefinition actionGroupDef = attachmentDef.GetActionGroup(node.actionGroupToTrigger);
				if(actionGroupDef.IsValid())
				{
					results.add(Pair.of(
						ActionGroupContext.CreateFrom(inputContext.Gun, prefix + node.actionGroupToTrigger),
						node.andContinueEvaluating));
					matched = true;
				}
			}

			if(matched && !node.andContinueEvaluating)
				break;
		}
	}
	// --------------------------------------------------------------------------
	// ACTIONS
	// --------------------------------------------------------------------------
	@Nonnull
	public ActionGroupInstance GetOrCreateActionGroup(ActionGroupContext context)
	{
		ActionStack stack = context.Gun.GetActionStack();
		for(ActionGroupInstance instance : stack.GetActiveActionGroups())
			if(instance.Def.key.equals(context.GroupPath))
				return instance;
		return CreateActionGroup(context);
	}
	@Nonnull
	public ActionGroupInstance CreateActionGroup(ActionGroupContext context)
	{
		ActionGroupInstance groupInstance = new ActionGroupInstance(context);
		for(ActionDefinition actionDef : context.Def.actions)
		{
			ActionInstance actionInstance = Actions.InstanceAction(groupInstance, actionDef);
			if(actionInstance != null)
				groupInstance.AddAction(actionInstance);
		}
		return groupInstance;
	}

	// --------------------------------------------------------------------------
	// PAINTJOBS
	// --------------------------------------------------------------------------
	@Nonnull
	public String GetPaintjobName()
	{
		return FlanItem.GetPaintjobName(GetItemStack());
	}

	public void SetPaintjobName(@Nonnull String paint)
	{
		ItemStack gunStack = GetItemStack();
		FlanItem.SetPaintjobName(gunStack, paint);
		SetItemStack(gunStack);
	}

	// --------------------------------------------------------------------------
	// ATTACHMENTS
	// --------------------------------------------------------------------------
	@Nonnull
	public AttachmentDefinition GetAttachmentDefinition(EAttachmentType attachType, int slot)
	{
		ItemStack attachmentStack = GetAttachmentStack(attachType, slot);
		if(attachmentStack.getItem() instanceof AttachmentItem attachmentItem)
		{
			return attachmentItem.Def();
		}
		return AttachmentDefinition.INVALID;
	}
	@Nonnull
	public List<AttachmentDefinition> GetAttachmentDefinitions()
	{
		List<AttachmentDefinition> defs = new ArrayList<>();
		for(ItemStack stack : GetAttachmentStacks())
		{
			if(stack.getItem() instanceof AttachmentItem attachmentItem)
				defs.add(attachmentItem.Def());
		}
		return defs;
	}

	public int GetNumAttachmentStacks(EAttachmentType attachType)
	{
		return Def.GetAttachmentSettings(attachType).numAttachmentSlots;
	}
	@Nonnull
	public ItemStack GetAttachmentStack(EAttachmentType attachType, int slot)
	{
		return FlanItem.GetAttachmentInSlot(GetItemStack(), attachType, slot);
	}

	public void SetAttachmentStack(EAttachmentType attachType, int slot, ItemStack attachmentStack)
	{
		ItemStack gunStack = GetItemStack();
		FlanItem.SetAttachmentInSlot(gunStack, attachType, slot, attachmentStack);
		SetItemStack(gunStack);
	}
	@Nonnull
	public ItemStack RemoveAttachmentFromSlot(EAttachmentType attachType, int slot)
	{
		ItemStack gunStack = GetItemStack();
		ItemStack removedStack = FlanItem.RemoveAttachmentFromSlot(gunStack, attachType, slot);
		SetItemStack(gunStack);
		return removedStack;
	}
	@Nonnull
	public List<ItemStack> GetAttachmentStacks()
	{
		return FlanItem.GetAttachmentStacks(GetItemStack());
	}
	// ---------------------------------------------------------------------------------------------------
	// MODIFIERS
	// ---------------------------------------------------------------------------------------------------
	private final ModifierCache ModCache;

	// Modifiers should be baked once per frame for a good balance of accuracy and performance
	public void BakeAllModifiers(@Nonnull IModifierBaker baker)
	{
		BakeModifiers(baker);
		BakeAttachmentModifiers(baker);
		BakeAbilityModifiers(baker);
	}
	private void BakeAttachmentModifiers(@Nonnull IModifierBaker baker)
	{
		for(ItemStack stack : GetAttachmentStacks())
			if(stack.getItem() instanceof AttachmentItem attachmentItem)
				for(ModifierDefinition modDef : attachmentItem.Def().modifiers)
					baker.Bake(modDef);
	}
	private void BakeAbilityModifiers(@Nonnull IModifierBaker baker)
	{
		for(var kvp : GetActiveModifierAbilities().entrySet())
			for(ModifierDefinition modDef : kvp.getKey().Def.modifiers)
				baker.Bake(modDef, kvp.getValue().Level, kvp.getValue().GetStackCount());
	}


	@Override
	public int GetNumAttachments()
	{
		return GetAttachmentStacks().size();
	}

	@Nonnull
	public StatAccumulator GetModifierFormula(@Nonnull String stat) { return ModCache.GetModifierFormula(stat); }
	@Nonnull
	public Optional<String> GetStringOverride(@Nonnull String stat) { return ModCache.GetStringOverride(stat); }

	@Nonnull
	public FloatAccumulation ModifyFloat(@Nonnull String stat)
	{
		return FloatAccumulation.compose(
			GetModifierFormula(stat).Calculate(this),
			GetShooter().GetModifierFormula(stat).Calculate(this));
	}
	@Nonnull
	public String ModifyString(@Nonnull String stat, @Nonnull String defaultValue)
	{
		return GetShooter().GetStringOverride(stat)
			.orElse(GetStringOverride(stat)
				.orElse(defaultValue));
	}


	//@Nonnull
	//public FloatModifier GetFloatModifier(@Nonnull String stat) { return ModCache.GetFloatModifier(stat); }
	//@Nonnull
	//public FloatModifier GetFloatModifier(@Nonnull String stat, @Nonnull String actionGroupPath) { return ModCache.GetFloatModifier(stat, actionGroupPath); }
	//@Nullable
	//public String GetModifiedString(@Nonnull String stat) { return ModCache.GetModifiedString(stat); }
	//@Nullable
	//public String GetModifiedString(@Nonnull String stat, @Nonnull String actionGroupPath) { return ModCache.GetModifiedString(stat, actionGroupPath); }
//
//
	//// Compose the gun modifiers and the shooter modifiers
	//public float GetFloat(@Nonnull String stat)
	//{
	//	return FloatModifier.of(GetFloatModifier(stat), GetShooter().GetFloatModifier(stat)).GetValue();
	//}
	//public float ModifyFloat(@Nonnull String stat, float baseValue)
	//{
	//	return FloatModifier.of(GetFloatModifier(stat), GetShooter().GetFloatModifier(stat)).ModifyValue(baseValue);
	//}
	//// If the gun sets this, return that override. Then check the shooter
	//@Nonnull
	//public String GetString(@Nonnull String stat)
	//{
	//	return Optional.ofNullable(GetModifiedString(stat)).orElse(GetShooter().GetString(stat));
	//}
	//@Nonnull
	//public String ModifyString(@Nonnull String stat, @Nonnull String defaultValue)
	//{
	//	return Optional.ofNullable(GetModifiedString(stat)).orElse(GetShooter().ModifyString(stat, defaultValue));
	//}

	// ---------------------------------------------------------------------------------------------------


	// -----------------------------------------------------------------------------------------------------------------
	// Traits & Abilities
	// -----------------------------------------------------------------------------------------------------------------
	@Nonnull
	public Map<CraftingTraitDefinition, Integer> GetTraits()
	{
		return FlanItem.GetTraits(GetItemStack());
	}

	@Nonnull
	public Map<AbilityInstanceApplyModifier, AbilityStack> GetActiveModifierAbilities()
	{
		Map<AbilityInstanceApplyModifier, AbilityStack> map = new HashMap<>();
		ForEachActiveModifierAbility(map::put);
		return map;
	}
	public void ForEachActiveModifierAbility(@Nonnull BiConsumer<AbilityInstanceApplyModifier, AbilityStack> func)
	{
		ActionStack actionStack = GetActionStack();
		if(actionStack.IsValid())
			ForEachAbility((abilityDef, tier) -> {
				if(abilityDef.IsStackable())
				{
					AbilityStack stacks = actionStack.GetStacks(abilityDef.stacking);
					if(stacks != null && stacks.IsActive())
					{
						for(AbilityEffectDefinition effectDef : abilityDef.effects)
						{
							if(effectDef.effectType == EAbilityEffect.ApplyModifier)
							{
								IAbilityEffect abilityEffect = effectDef.GetEffectProcessor();
								if(abilityEffect instanceof AbilityInstanceApplyModifier modifierAbility)
									func.accept(modifierAbility, stacks);
								else
									FlansMod.LOGGER.error("Ability was of type ApplyModifier, but had some other processor attached");
							}
						}
					}
				}
			});
	}

	@Nonnull
	public Map<AbilityDefinition, Integer> GetAbilities()
	{
		Map<AbilityDefinition, Integer> map = new HashMap<>();
		ForEachAbility(map::put);
		return map;
	}
	public void ForEachAbility(@Nonnull BiConsumer<AbilityDefinition, Integer> func)
	{
		// Apply static abilities (at level 1, they never level up)
		for(AbilityDefinition staticDef : Def.staticAbilities)
			func.accept(staticDef, 1);

		// And apply traits inherited from crafting inputs (accumulating levels)
		for(var kvp : GetTraits().entrySet())
		{
			for(AbilityDefinition traitAbility : kvp.getKey().abilities)
				func.accept(traitAbility, kvp.getValue());
		}
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Modes
	// -----------------------------------------------------------------------------------------------------------------

	@Nonnull
	public ModeDefinition[] GetAllModeDefs()
	{
		return Def.modes;
	}
	@Nullable
	public ModeDefinition GetModeDef(@Nonnull String modeKey)
	{
		for(ModeDefinition modeDef : Def.modes)
			if(modeDef.key.equals(modeKey))
				return modeDef;

		// TODO: Modal toggles on attachments

		return null;
	}
	@Nonnull
	public String GetDefaultModeValue(@Nonnull String modeKey)
	{
		ModeDefinition modeDef = GetModeDef(modeKey);
		if(modeDef != null)
			return modeDef.defaultValue;
		return "";
	}
	@Nonnull
	public String GetModeValue(@Nonnull String modeKey)
	{
		return FlanItem.GetModeValue(GetItemStack(), modeKey, GetDefaultModeValue(modeKey));
	}
	public void SetModeValue(@Nonnull String modeKey, @Nonnull String modeValue)
	{
		ItemStack stack = GetItemStack();
		FlanItem.SetModeValue(stack, modeKey, modeValue);
		SetItemStack(stack);
		GetActionStack().EvaluateTrigger(EAbilityTrigger.SwitchMode, this, TriggerContext.self(this));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Util methods
	// -----------------------------------------------------------------------------------------------------------------

	public boolean ExpelItems(@Nonnull List<ItemStack> stacks)
	{
		Level level = GetLevel();
		if(level == null)
			return false;
		if(level.isClientSide)
			return false;

		for(ItemStack stack : stacks)
		{
			ItemEntity itemEntity = new ItemEntity(EntityType.ITEM, level);
			itemEntity.setItem(stack);
			itemEntity.setPos(GetShootOrigin().PositionVec3());
			level.addFreshEntity(itemEntity);
		}

		return true;
	}

	public void Save(CompoundTag tags)
	{
		// Save a copy of the ItemStack
		CompoundTag stackTags = new CompoundTag();
		Stack.save(stackTags);
		tags.put("stack", stackTags);

		// Then put extra details if they make sense
		tags.putInt("slot", GetInventorySlotIndex());

		// And if our shooter is set, write that out
		// TODO: Should this not be more like a heirarchy where these are child NBT nodes of shooter/null?
		CompoundTag shooterTags = new CompoundTag();
		GetShooter().Save(shooterTags);
		tags.put("shooter", shooterTags);
	}

	public static GunContext Load(CompoundTag tags, boolean client)
	{
		// Stack
		ItemStack stack = ItemStack.of(tags.getCompound("stack"));
		UUID gunID = FlanItem.GetGunID(stack);

		// Slot
		int slot = tags.getInt("slot");

		// Shooter
		ShooterContext shooter = ShooterContext.Load(tags.getCompound("shooter"), client);
		if(shooter.IsValid())
			return shooter.CreateContext(gunID);

		// Last option, we don't know who made this, but we can give an isolated context to allow _some_ functionality
		return ContextCache.CreateWithoutCaching(stack);
	}

	@Override
	public String toString()
	{
		return "GunContext:" + GetItemStack().toString();
	}
}
