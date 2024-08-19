package com.flansmod.common.item;

import com.flansmod.client.FlansModClient;
import com.flansmod.client.render.guns.GunItemClientExtension;
import com.flansmod.client.render.guns.GunItemRenderer;
import com.flansmod.common.FlansMod;
import com.flansmod.common.abilities.AbilityEffectProvideEnchantment;
import com.flansmod.common.actions.*;
import com.flansmod.common.actions.contexts.*;
import com.flansmod.common.types.Constants;
import com.flansmod.common.types.abilities.elements.EAbilityTarget;
import com.flansmod.common.types.bullets.BulletDefinition;
import com.flansmod.common.types.guns.elements.*;
import com.flansmod.common.types.elements.ModifierDefinition;
import com.flansmod.common.types.guns.GunDefinition;
import com.flansmod.common.types.magazines.MagazineDefinition;
import com.flansmod.util.Maths;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.common.TierSortingRegistry;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

public class GunItem extends FlanItem
{
    public GunDefinition Def() { return FlansMod.GUNS.Get(DefinitionLocation); }

    public GunItem(@Nonnull ResourceLocation defLoc, @Nonnull Properties properties)
    {
        super(defLoc, properties);
    }

    @Override
    public void appendHoverText(@Nonnull ItemStack stack,
                                @Nullable Level level,
                                @NotNull List<Component> tooltips,
                                @Nonnull TooltipFlag flags)
    {
        GunContext gunContext = GunContext.of(stack, EContextSide.of(level));
        if(gunContext.IsValid())
        {
            ActionGroupContext actionContext = ActionGroupContext.CreateFrom(gunContext, Actions.DefaultPrimaryActionKey);
            if (actionContext.IsValid())
            {
                boolean advanced = flags.isAdvanced();
                boolean expanded = InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), InputConstants.KEY_RSHIFT)
                    || InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), InputConstants.KEY_LSHIFT);

                Component fireRateString = actionContext.RepeatMode() == ERepeatMode.SemiAuto ?
                    Component.translatable("tooltip.format.singlefire") :
                    Component.translatable("tooltip.format.fullautorpm", actionContext.RoundsPerMinute());

                // To calculate base "gun stats" without taking the bullet into consideration, assume a bullet with default stats
                GunshotContext gunshotContext = GunshotContext.hitscan(actionContext, BulletDefinition.STANDARD_TEST_BULLET, 0);
                if(expanded)
                {
                    tooltips.add(Component.translatable("tooltip.format."+ Constants.STAT_IMPACT_DAMAGE + ".advanced", gunshotContext.EstimateImpactDamage(EAbilityTarget.ShotEntity)));
                    tooltips.add(Component.translatable("tooltip.format."+ Constants.STAT_SHOT_VERTICAL_RECOIL + ".advanced", actionContext.VerticalRecoil()));
                    tooltips.add(Component.translatable("tooltip.format."+ Constants.STAT_SHOT_SPREAD + ".advanced", actionContext.Spread()));
                }
                else
                {
                    tooltips.add(Component.translatable(
                        "tooltip.format.primarystatline",
                        gunshotContext.EstimateImpactDamage(EAbilityTarget.ShotEntity),
                        fireRateString,
                        actionContext.VerticalRecoil(),
                        actionContext.Spread()));
                }

                if(expanded)
                {
                    switch(actionContext.RepeatMode())
                    {
                        case Toggle -> { tooltips.add(Component.translatable("tooltip.format.toggle.advanced")); }
                        case FullAuto -> { tooltips.add(Component.translatable("tooltip.format.fullautorpm.advanced", actionContext.RoundsPerMinute())); }
                        case SemiAuto -> { tooltips.add(Component.translatable("tooltip.format.singlefire.advanced")); }
                        case Minigun -> { tooltips.add(Component.translatable("tooltip.format.minigunrpm.advanced", actionContext.RoundsPerMinute())); }
                        case BurstFire -> { tooltips.add(Component.translatable("tooltip.format.burstfirerpm.advanced", actionContext.RoundsPerMinute())); }
                    }
                }

                MagazineDefinition magDef = actionContext.GetMagazineType(0);
                if(magDef.IsValid())
                {
                    tooltips.add(Component.translatable("magazine." + magDef.Location.getNamespace() + "." + magDef.Location.getPath()));
                }
                int primaryBullets = actionContext.GetMagazineSize(0);
                if(primaryBullets == 1)
                {
                    ItemStack bulletStack = actionContext.GetBulletAtIndex(0, 0);
                    if(!bulletStack.isEmpty())
                    {
                         tooltips.add(Component.translatable("tooltip.format.single_bullet_stack", bulletStack.getHoverName()));
                    }
                }
                else
                {

                    HashMap<Item, ItemStack> bulletCounts = new HashMap<>();
                    for (int i = 0; i < primaryBullets; i++)
                    {
                        ItemStack bulletStack = actionContext.GetBulletAtIndex(0, i);
                        if(!bulletStack.isEmpty())
                        {
                            if (!bulletCounts.containsKey(bulletStack.getItem()))
                            {
                                bulletCounts.put(bulletStack.getItem(), bulletStack.copy());
                            } else
                            {
                                bulletCounts.replace(bulletStack.getItem(), bulletStack.copyWithCount(bulletCounts.get(bulletStack.getItem()).getCount() + 1));
                            }
                        }
                    }
                    for(var kvp : bulletCounts.entrySet())
                    {
                        tooltips.add(Component.translatable("tooltip.format.multiple_bullet_stack", kvp.getValue().getCount(), kvp.getValue().getHoverName()));
                    }
                }
            }
        }

        super.appendHoverText(stack, level, tooltips, flags);
    }

    @Nonnull
    public CompoundTag GetRootTag(@Nonnull ItemStack stack, @Nonnull String groupPath)
    {
        String rootTagName = groupPath;
        GunContext gunContext = ContextCache.CreateWithoutCaching(stack);
        if(gunContext.IsValid())
        {
            ActionGroupContext actionGroupContext = gunContext.GetActionGroupContext(groupPath);
            ReloadDefinition reloadDef = gunContext.GetReloadDefinitionContaining(actionGroupContext);
            if(reloadDef != null)
            {
                rootTagName = reloadDef.key;
            }
        }
        if(!stack.getOrCreateTag().contains(rootTagName))
            stack.getOrCreateTag().put(rootTagName, new CompoundTag());

        return stack.getOrCreateTag().getCompound(rootTagName);
    }

    @Nonnull
    public CompoundTag GetMagTag(@Nonnull ItemStack stack, @Nonnull String groupPath, int magIndex)
    {
        CompoundTag rootTag = GetRootTag(stack, groupPath);
        final String magTag = "mag_" + magIndex;
        if (!rootTag.contains(magTag))
            rootTag.put(magTag, new CompoundTag());
        return rootTag.getCompound(magTag);
    }
    @Nonnull
    public MagazineDefinition GetMagazineType(ItemStack stack, String groupPath, int magIndex)
    {
        // Get the root tag for our magazine
        CompoundTag magTags = GetMagTag(stack, groupPath, magIndex);
        if(magTags.contains("type"))
        {
            String type = magTags.getString("type");
            ResourceLocation magLoc = new ResourceLocation(type);
            return FlansMod.MAGAZINES.Get(magLoc);
        }
        List<MagazineDefinition> matches = Def().GetMagazineSettings(groupPath).GetMatchingMagazines();
        if(matches.size() > 0)
        {
            return matches.get(0);
        }
        return MagazineDefinition.INVALID;
    }
    @Nonnull
    public ItemStack[] GetCombinedBulletStacks(@Nonnull ItemStack stack, @Nonnull String groupPath, int magIndex)
    {
        CompoundTag magTags = GetMagTag(stack, groupPath, magIndex);
        if(magTags.contains("bullets"))
        {
            CompoundTag bulletTags = magTags.getCompound("bullets");
            ItemStack[] stacks = new ItemStack[bulletTags.size()];
            int stackIndex = 0;
            for(String key : bulletTags.getAllKeys())
            {
                stacks[stackIndex] = ItemStack.of(bulletTags.getCompound(key));
                stackIndex++;
            }
            return stacks;
        }
        return new ItemStack[0];
    }
    @Nonnull
    public ItemStack GetBulletAtIndex(@Nonnull ItemStack stack, @Nonnull String groupPath, int magIndex, int bulletIndex)
    {
        CompoundTag magTags = GetMagTag(stack, groupPath, magIndex);
        if (magTags.contains("bullets"))
        {
            CompoundTag bulletTags = magTags.getCompound("bullets");
            for(String key : bulletTags.getAllKeys())
            {
                int startIndex = Integer.parseInt(key);
                ItemStack bulletStack = ItemStack.of(bulletTags.getCompound(key));

                int endIndex = startIndex + bulletStack.getCount();
                if(startIndex <= bulletIndex && bulletIndex < endIndex)
                {
                    // Apple represents empty spaces because Minecraft hides 5xAir as 0xAir
                    if(bulletStack.getItem() == Items.APPLE)
                        return ItemStack.EMPTY;

                    return bulletStack.copyWithCount(1);
                }
            }
        }
        return ItemStack.EMPTY;
    }
    public int GetNumBulletsInMag(@Nonnull ItemStack stack, @Nonnull String groupPath, int magIndex)
    {
        int count = 0;
        CompoundTag magTags = GetMagTag(stack, groupPath, magIndex);
        if (magTags.contains("bullets"))
        {
            CompoundTag bulletTags = magTags.getCompound("bullets");
            for(String key : bulletTags.getAllKeys())
            {
                int startIndex = Integer.parseInt(key);
                ItemStack bulletStack = ItemStack.of(bulletTags.getCompound(key));

                // Apple represents empty spaces because Minecraft hides 5xAir as 0xAir
                if(bulletStack.getItem() == Items.APPLE)
                    continue;

                count += bulletStack.getCount();
            }
        }
        return count;
    }
    @Nonnull
    public Item[] ExtractCompactStacks(@Nonnull ItemStack stack, @Nonnull String groupPath, int magIndex)
    {
        MagazineDefinition magDef = GetMagazineType(stack, groupPath, magIndex);
        CompoundTag magTags = GetMagTag(stack, groupPath, magIndex);
        Item[] items = new Item[magDef.numRounds];
        for(int i = 0; i < magDef.numRounds; i++)
            items[i] = Items.APPLE;

        if (magTags.contains("bullets"))
        {
            CompoundTag bulletTags = magTags.getCompound("bullets");
            for(String key : bulletTags.getAllKeys())
            {
                int startIndex = Integer.parseInt(key);
                ItemStack bulletStack = ItemStack.of(bulletTags.getCompound(key));
                int endIndex = startIndex + bulletStack.getCount();
                for(int i = startIndex; i < endIndex; i++)
                {
                    if(0 <= i && i < magDef.numRounds)
                        items[i] = bulletStack.getItem();
                }
            }
        }
        return items;
    }
    @OnlyIn(Dist.CLIENT)
    public void ClientHandleMouse(Player player, ItemStack stack, InputEvent.InteractionKeyMappingTriggered event)
    {
        if(event.isAttack()) // Primary actions
        {
          //  FlansModClient.ACTIONS_CLIENT.ClientInputEvent(player, event.getHand(), EActionInput.PRIMARY);
            //player.startUsingItem(event.getHand());
        }
        else if(event.isUseItem()) // Secondary actions
        {
            //FlansModClient.ACTIONS_CLIENT.ClientInputEvent(player, event.getHand(), EActionInput.SECONDARY);
        }
    }
    @OnlyIn(Dist.CLIENT)
    public void ClientUpdateUsing(Player player, ItemStack stack, LivingEntityUseItemEvent.Tick event)
    {
       // int useRemaining = FlansModClient.ACTIONS_CLIENT.ClientInputHeldUpdate(player, event.getEntity().getUsedItemHand());

       // event.setDuration(useRemaining);
    }

    // Left-click vanilla actions
    @Override
    public boolean canAttackBlock(@Nonnull BlockState blockState, @Nonnull Level world, @Nonnull BlockPos blockPos, Player player)
    {
        if(player.isCreative())
            return false;

        ShooterContext shooterContext = ShooterContext.of(player);
        if(shooterContext.IsValid())
        {
            for(GunContext gunContext : shooterContext.GetAllGunContexts(world.isClientSide))
            {
                for (ActionDefinition actionDef : gunContext.GetPotentialPrimaryActions())
                {
                    switch (actionDef.actionType)
                    {
                        case Axe, Pickaxe, Hoe, Shovel, Melee -> {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }
    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState blockState)
    {
        List<Tier> tiers = TierSortingRegistry.getSortedTiers();
        GunContext gunContext = ContextCache.CreateWithoutCaching(stack);
        for (ActionDefinition actionDef : gunContext.GetPotentialPrimaryActions())
        {
            int harvestLevel = Maths.Ceil(gunContext.ModifyFloat(Constants.STAT_TOOL_HARVEST_LEVEL).get());
            switch (actionDef.actionType)
            {
                case Melee -> { return blockState.is(Blocks.COBWEB); }
                case Axe ->
                {
                    if(blockState.is(BlockTags.MINEABLE_WITH_AXE))
                        for(Tier tier : tiers)
                            if(tier.getLevel() >= harvestLevel)
                                if(TierSortingRegistry.isCorrectTierForDrops(tier, blockState))
                                    return true;
                }
                case Pickaxe ->
                {
                    if(blockState.is(BlockTags.MINEABLE_WITH_PICKAXE))
                        for(Tier tier : tiers)
                            if(tier.getLevel() >= harvestLevel)
                                if(TierSortingRegistry.isCorrectTierForDrops(tier, blockState))
                                    return true;
                }
                case Hoe ->
                {
                    if(blockState.is(BlockTags.MINEABLE_WITH_HOE))
                        for(Tier tier : tiers)
                            if(tier.getLevel() >= harvestLevel)
                                if(TierSortingRegistry.isCorrectTierForDrops(tier, blockState))
                                    return true;
                }
                case Shovel ->
                {
                    if(blockState.is(BlockTags.MINEABLE_WITH_SHOVEL))
                        for(Tier tier : tiers)
                            if(tier.getLevel() >= harvestLevel)
                                if(TierSortingRegistry.isCorrectTierForDrops(tier, blockState))
                                    return true;
                }
            }
        }
        return false;
    }
    @Override
    public void inventoryTick(@Nonnull ItemStack stack, Level level, @Nonnull Entity entity, int i, boolean b)
    {
        if(level.isClientSide)
        {
            if(entity instanceof Player player)
            {
                if (player.getInventory().selected == i)
                {
                    // If we have a vanilla left-click action, don't do anything
                    GunContext gunContext = GunContext.of(stack, EContextSide.of(level));
                    for (ActionDefinition actionDef : gunContext.GetPotentialPrimaryActions())
                    {
                        switch (actionDef.actionType)
                        {
                            case Axe, Pickaxe, Hoe, Shovel, Melee -> {
                                return;
                            }
                        }
                    }

                    // Otherwise, block input
                    FlansModClient.SetMissTime(10);
                }
            }
        }

    }
    @Override
    public int getEnchantmentLevel(@Nonnull ItemStack stack, @Nonnull Enchantment enchantment)
    {
        int highestLevel = 0;
        GunContext gun = GunContext.of(stack);
        if(gun.IsValid())
        {
            for(var kvp : gun.GetActiveModifierAbilities().entrySet())
            {
                if(kvp.getKey().Def.GetEffectProcessor() instanceof AbilityEffectProvideEnchantment enchantmentProvider)
                {
                    if(enchantmentProvider.Enchant.equals(enchantment))
                    {
                        int level = enchantmentProvider.GetLevel(gun.GetActionGroupContext(Actions.DefaultPrimaryActionKey), kvp.getValue());
                        if(level > highestLevel)
                            highestLevel = level;
                    }
                }
            }
        }

        return Maths.Max(highestLevel, super.getEnchantmentLevel(stack, enchantment));
    }
    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack)
    {
        GunContext gunContext = ContextCache.CreateWithoutCaching(stack);
        if(gunContext.IsValid())
        {
            ActionGroupContext actionGroupContext = gunContext.GetActionGroupContext(Actions.DefaultPrimaryActionKey);
            if(actionGroupContext.IsValid())
            {
                ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
                for(ActionDefinition actionDef : gunContext.GetPotentialPrimaryActions())
                {
                    if(actionDef.actionType == EActionType.Melee)
                    {
                        float meleeDamage = actionGroupContext.ModifyFloat(Constants.STAT_MELEE_DAMAGE).get();
                        builder.put(Attributes.ATTACK_DAMAGE,
                            new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Gun modifier", meleeDamage, AttributeModifier.Operation.ADDITION));
                    }
                }
                return builder.build();
            }
        }
        return ImmutableMultimap.of();
    }
    // This is a response event to when this has been used in a melee attack, not the code to create a melee attack
    @Override
    public boolean hurtEnemy(ItemStack stack, @Nonnull LivingEntity wielder, @Nonnull LivingEntity victim)
    {
        stack.hurtAndBreak(2, victim, (entity) -> {
            entity.broadcastBreakEvent(EquipmentSlot.MAINHAND);
        });
        return true;
    }
    // This is a response event to when this has been used to mine a block, not the code to mine a block
    @Override
    public boolean mineBlock(@Nonnull ItemStack stack, Level level, @Nonnull BlockState blockState, @Nonnull BlockPos blockPos, @Nonnull LivingEntity wielder)
    {
        if (!level.isClientSide && blockState.getDestroySpeed(level, blockPos) != 0.0F)
        {
            stack.hurtAndBreak(1, wielder, (entity) -> {
                entity.broadcastBreakEvent(EquipmentSlot.MAINHAND);
            });
        }
        return true;
    }

    // Right-click vanilla actions
    @Override
    @Nonnull
    public UseAnim getUseAnimation(@Nonnull ItemStack stack)
    {
        GunContext gunContext = ContextCache.CreateWithoutCaching(stack);
        for (ActionDefinition actionDef : gunContext.GetPotentialSecondaryActions())
        {
            switch (actionDef.actionType)
            {
                case Shield -> { return UseAnim.BLOCK; }
            }
        }
        return UseAnim.NONE;
    }
    @Override
    public int getUseDuration(@Nonnull ItemStack stack)
    {
        GunContext gunContext = ContextCache.CreateWithoutCaching(stack);
        for (ActionDefinition actionDef : gunContext.GetPotentialSecondaryActions())
        {
            switch (actionDef.actionType)
            {
                case Shield -> { return 72000; }
            }
        }
        return 0;
    }
    @Nonnull
    @Override
    public InteractionResultHolder<ItemStack> use(@Nonnull Level world, @Nonnull Player player, @Nonnull InteractionHand hand)
    {
        ShooterContext shooterContext = ShooterContext.of(player);
        if(shooterContext.IsValid() && shooterContext instanceof ShooterContextPlayer playerContext)
        {
            GunContext gunContext = playerContext.GetGunContextForSlot(hand, world.isClientSide);
            for (ActionDefinition actionDef : gunContext.GetPotentialSecondaryActions())
            {
                switch (actionDef.actionType)
                {
                    case Shield -> {
                        ItemStack stackInHand = player.getItemInHand(hand);
                        player.startUsingItem(hand);
                        return InteractionResultHolder.consume(stackInHand);
                    }
                }
            }
        }
        ItemStack itemstack = player.getItemInHand(hand);
        return InteractionResultHolder.pass(itemstack);
    }
    @Nonnull
    @Override
    public InteractionResult useOn(UseOnContext context)
    {
        ShooterContext shooter = ShooterContext.of(context.getPlayer());
        if(shooter.IsValid() && shooter instanceof ShooterContextPlayer playerContext)
        {
            GunContext gunContext = playerContext.GetGunContextForSlot(context.getHand(), context.getLevel().isClientSide);
            if (gunContext.IsValid())
            {
                ActionStack actionStack = gunContext.GetActionStack();
                // Always secondary, this is a use action
                ActionGroupContext actionGroupContext = gunContext.GetActionGroupContext("secondary");
                ActionGroupInstance actionGroup = actionStack.GetOrCreateGroupInstance(actionGroupContext);
                EActionResult startResult = actionGroup.CanStart();
                if (startResult == EActionResult.CanProcess)
                {
                    for (ActionInstance action : actionGroup.GetActions())
                    {
                        switch (action.Def.actionType)
                        {
                            case Strip -> {
                                return Items.WOODEN_AXE.useOn(context);
                            }
                            case Shear -> {
                                return Items.SHEARS.useOn(context);
                            }
                            case Flatten -> {
                                return Items.WOODEN_SHOVEL.useOn(context);
                            }
                            case Till -> {
                                return Items.WOODEN_HOE.useOn(context);
                            }
                        }
                    }
                }
                actionStack.CancelGroupInstance(actionGroupContext);
            }
        }
        return InteractionResult.CONSUME;
    }


    @Override
    public boolean canPerformAction(@Nonnull ItemStack stack, @Nonnull ToolAction toolAction)
    {
        // We do not currently support checking for vanilla actions in attachments and other sources.
        // Only base actions will be returned.
        // This is because "canPerformAction" is polled very regularly and provides no context with which to cache
        for (ActionGroupDefinition actionGroup : Def().actionGroups)
        {
            for(ActionDefinition actionDef : actionGroup.actions)
            {
                switch(actionDef.actionType)
                {
                    case Strip -> { if(toolAction == ToolActions.AXE_STRIP) return true; }
                    case Shear -> { if(toolAction == ToolActions.SHEARS_HARVEST) return true; }
                    case Flatten -> { if(toolAction == ToolActions.SHOVEL_FLATTEN) return true; }
                    case Till -> { if(toolAction == ToolActions.HOE_TILL) return true; }
                    case Axe -> { if(toolAction == ToolActions.AXE_DIG) return true; }
                    case Pickaxe -> { if(toolAction == ToolActions.PICKAXE_DIG) return true; }
                    case Shovel -> { if(toolAction == ToolActions.SHOVEL_DIG) return true; }
                    case Hoe -> { if(toolAction == ToolActions.HOE_DIG) return true; }
                    case Shield -> { if(toolAction == ToolActions.SHIELD_BLOCK) return true; }
                }
            }
        }

        return false;
    }


    @Override
    @OnlyIn(Dist.CLIENT)
    public void initializeClient(@Nonnull Consumer<IClientItemExtensions> consumer)
    {
        consumer.accept(GunItemClientExtension.of(this));
    }

    // Random parameter overrides
    public boolean isEnchantable(ItemStack i) { return false; }
}
