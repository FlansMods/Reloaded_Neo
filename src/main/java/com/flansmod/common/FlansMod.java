package com.flansmod.common;

import com.flansmod.client.FlansModClient;
import com.flansmod.common.actions.contexts.*;
import com.flansmod.common.actions.ServerActionManager;
import com.flansmod.common.crafting.*;
import com.flansmod.common.crafting.ingredients.StackedVanillaIngredient;
import com.flansmod.common.crafting.ingredients.TieredMaterialIngredient;
import com.flansmod.common.crafting.ingredients.TieredPartIngredient;
import com.flansmod.common.crafting.menus.*;
import com.flansmod.common.crafting.recipes.GunFabricationRecipe;
import com.flansmod.common.crafting.recipes.PartFabricationRecipe;
import com.flansmod.common.entity.NpcRelationshipCapabilityAttacher;
import com.flansmod.common.entity.longdistance.LongDistanceEntitySystem;
import com.flansmod.common.entity.longdistance.LongDistanceVehicle;
import com.flansmod.common.entity.longdistance.ServerLongDistanceEntitySystem;
import com.flansmod.common.entity.vehicle.VehicleEntity;
import com.flansmod.common.entity.vehicle.hierarchy.WheelEntity;
import com.flansmod.common.gunshots.Raytracer;
import com.flansmod.common.item.*;
import com.flansmod.common.network.FlansEntityDataSerializers;
import com.flansmod.common.network.FlansModPacketHandler;
import com.flansmod.common.projectiles.BulletEntity;
import com.flansmod.common.types.abilities.CraftingTraitDefinition;
import com.flansmod.common.types.abilities.CraftingTraitDefinitions;
import com.flansmod.common.types.abilities.elements.AbilityEffectDefinition;
import com.flansmod.common.types.abilities.elements.CraftingTraitProviderDefinition;
import com.flansmod.common.types.abilities.elements.EAbilityEffect;
import com.flansmod.common.types.armour.ArmourDefinitions;
import com.flansmod.common.types.attachments.AttachmentDefinitions;
import com.flansmod.common.types.attachments.EAttachmentType;
import com.flansmod.common.types.crafting.MaterialDefinitions;
import com.flansmod.common.types.crafting.WorkbenchDefinitions;
import com.flansmod.common.types.grenades.GrenadeDefinitions;
import com.flansmod.common.types.guns.GunDefinitions;
import com.flansmod.common.types.bullets.BulletDefinitions;
import com.flansmod.common.types.guns.elements.AbilityDefinition;
import com.flansmod.common.types.magazines.MagazineDefinitions;
import com.flansmod.common.types.npc.NpcDefinitions;
import com.flansmod.common.types.parts.PartDefinitions;
import com.flansmod.common.types.vehicles.ControlSchemeDefinitions;
import com.flansmod.common.types.vehicles.VehicleDefinitions;
import com.flansmod.common.worldgen.loot.LootPopulator;
import com.flansmod.util.Transform;
import com.flansmod.util.collision.OBBCollisionSystem;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.stats.Stats;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.RegisterRecipeBookCategoriesEvent;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.crafting.CraftingHelper;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings("unused")
@Mod(FlansMod.MODID)
public class FlansMod
{
    public static final String MODID = "flansmod";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static boolean DEBUG = false;

    // Registers
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(BuiltInRegistries.BLOCK, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, MODID);
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(BuiltInRegistries.RECIPE_TYPE, MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, MODID);
    public static final DeferredRegister<BlockEntityType<?>> TILE_ENTITIES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(BuiltInRegistries.MENU, MODID);
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIERS = DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(Registries.ATTRIBUTE, MODID);

    // Core mod blocks & items
    public static final Supplier<EntityType<BulletEntity>> ENT_TYPE_BULLET = ENTITY_TYPES.register(
        "bullet",
        () -> EntityType.Builder.of(
            BulletEntity::new,
            MobCategory.MISC)
            .sized(0.5f, 0.5f)
            .build("bullet"));
    public static final Supplier<EntityType<WheelEntity>> ENT_TYPE_WHEEL = ENTITY_TYPES.register(
        "wheel",
        () -> EntityType.Builder.of(
            WheelEntity::new,
            MobCategory.MISC)
            .sized(0.25f, 0.25f)
            .build("wheel"));


    // PartEntitys don't get registered?
    //public static final RegistryObject<EntityType<WheelEntity>> ENT_TYPE_WHEEL = ENTITY_TYPES.register(
    //    "wheel",
    //    () -> EntityType.Builder.of(
    //            WheelEntity::new,
    //            MobCategory.MISC)
    //        .sized(0.5f, 0.5f)
    //        .build("wheel"));

    public static final Supplier<Item> RAINBOW_PAINT_CAN_ITEM = ITEMS.register("rainbow_paint_can", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> MAG_UPGRADE_ITEM = ITEMS.register("magazine_upgrade", () -> new Item(new Item.Properties()));

    public static final Supplier<Block> GUN_MOD_TABLE_BLOCK = FlansMod.Workbench_Block(BLOCKS, MODID, "gun_modification_table");
    public static final Supplier<Block> DIESEL_GENERATOR_BLOCK = FlansMod.Workbench_Block(BLOCKS, MODID, "portable_diesel_generator");
    public static final Supplier<Block> COAL_GENERATOR_BLOCK = FlansMod.Workbench_Block(BLOCKS, MODID, "portable_coal_generator");
    public static final Supplier<Item> GUN_MOD_TABLE_ITEM = ITEMS.register("gun_modification_table", () -> new BlockItem(GUN_MOD_TABLE_BLOCK.get(), new Item.Properties()));
    public static final Supplier<Item> DIESEL_GENERATOR_ITEM = ITEMS.register("portable_diesel_generator", () -> new BlockItem(DIESEL_GENERATOR_BLOCK.get(), new Item.Properties()));
    public static final Supplier<Item> COAL_GENERATOR_ITEM = ITEMS.register("portable_coal_generator", () -> new BlockItem(COAL_GENERATOR_BLOCK.get(), new Item.Properties()));
    public static final Supplier<Item> GUN_MOD_PORTABLE_ITEM = FlansMod.Workbench_Quick_Item(ITEMS, MODID, "portable_mod_kit");

    // Tile entities
    public static final Supplier<BlockEntityType<WorkbenchBlockEntity>> DIESEL_GENERATOR_TILE_ENTITY = Workbench_TileEntityType(TILE_ENTITIES, MODID, "portable_diesel_generator");
    public static final Supplier<BlockEntityType<WorkbenchBlockEntity>> COAL_GENERATOR_TILE_ENTITY = Workbench_TileEntityType(TILE_ENTITIES, MODID, "portable_coal_generator");
    public static final Supplier<BlockEntityType<WorkbenchBlockEntity>> GUN_MODIFICATION_TILE_ENTITY = Workbench_TileEntityType(TILE_ENTITIES, MODID, "gun_modification_table");

    // Menus
    public static final Supplier<MenuType<WorkbenchMenuGunCrafting>> WORKBENCH_MENU_GUN_CRAFTING = MENUS.register("workbench_gun_crafting", () -> IMenuTypeExtension.create(WorkbenchMenuGunCrafting::new));
    public static final Supplier<MenuType<WorkbenchMenuPower>> WORKBENCH_MENU_POWER = MENUS.register("workbench_power", () -> IMenuTypeExtension.create(WorkbenchMenuPower::new));
    public static final Supplier<MenuType<WorkbenchMenuMaterials>> WORKBENCH_MENU_MATERIALS = MENUS.register("workbench_materials", () -> IMenuTypeExtension.create(WorkbenchMenuMaterials::new));
    public static final Supplier<MenuType<WorkbenchMenuModification>> WORKBENCH_MENU_MODIFICATION  = MENUS.register("workbench_modification", () -> IMenuTypeExtension.create(WorkbenchMenuModification::new));
    public static final Supplier<MenuType<WorkbenchMenuPartCrafting>> WORKBENCH_MENU_PART_CRAFTING = MENUS.register("workbench_part_crafting", () -> IMenuTypeExtension.create(WorkbenchMenuPartCrafting::new));

    // Recipes
    public static final Supplier<RecipeType<PartFabricationRecipe>> PART_FABRICATION_RECIPE_TYPE = RECIPE_TYPES.register("part_fabrication", () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(MODID, "part_fabrication")));
    public static final Supplier<RecipeSerializer<PartFabricationRecipe>> PART_FABRICATION_RECIPE_SERIALIZER = RECIPE_SERIALIZERS.register("part_fabrication", PartFabricationRecipe.Serializer::new);
    public static final Supplier<RecipeType<GunFabricationRecipe>> GUN_FABRICATION_RECIPE_TYPE = RECIPE_TYPES.register("gun_fabrication", () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(MODID, "gun_fabrication")));
    public static final Supplier<RecipeSerializer<GunFabricationRecipe>> GUN_FABRICATION_RECIPE_SERIALIZER = RECIPE_SERIALIZERS.register("gun_fabrication", GunFabricationRecipe.Serializer::new);

    // Attributes
    public static final Supplier<RangedAttribute> IMPACT_DAMAGE_MULTIPLIER = ATTRIBUTES.register("impact_damage_multiplier", () -> new RangedAttribute("impact_damage_multiplier", 1d, 0d, 2048d));
    public static final Supplier<RangedAttribute> SPLASH_RADIUS_MULTIPLIER = ATTRIBUTES.register("splash_radius_multiplier", () -> new RangedAttribute("splash_radius_multiplier", 1d, 0d, 2048d));
    public static final Supplier<RangedAttribute> TIME_BETWEEN_SHOTS_MULTIPLIER = ATTRIBUTES.register("time_between_shots_multiplier", () -> new RangedAttribute("time_between_shots_multiplier", 1d, 0.1d, 2048d));
    public static final Supplier<RangedAttribute> SHOT_SPREAD_MULTIPLIER = ATTRIBUTES.register("shot_spread_multiplier", () -> new RangedAttribute("shot_spread_multiplier", 1d, 0d, 2048d));
    public static final Supplier<RangedAttribute> VERTICAL_RECOIL_MULTIPLIER = ATTRIBUTES.register("vertical_recoil_multiplier", () -> new RangedAttribute("vertical_recoil_multiplier", 1d, 0d, 2048d));
    public static final Supplier<RangedAttribute> HORIZONTAL_RECOIL_MULTIPLIER = ATTRIBUTES.register("horizontal_recoil_multiplier", () -> new RangedAttribute("horizontal_recoil_multiplier", 1d, 0d, 2048d));


    // Loot Modifiers
    public static final Supplier<Codec<LootPopulator>> LOOT_POPULATOR = LOOT_MODIFIERS.register("loot_populator", LootPopulator.CODEC);

    // Damage Types
    public static final ResourceKey<DamageType> DAMAGE_TYPE_GUN = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(MODID, "gun"));

    // Creative Tabs
    public static final Component CREATIVE_TAB_NAME_GUNS = Component.translatable("item_group." + MODID + ".creative_tab_guns");
    public static final Supplier<CreativeModeTab> CREATIVE_TAB_GUNS = CREATIVE_TABS.register("creative_tab_guns",
        () -> {
            List<ItemStack> stacks = new ArrayList<>();
            for(Item item : BuiltInRegistries.ITEM)
            {
                if(item instanceof GunItem)
                    stacks.add(new ItemStack(item));
            }

            return CreativeModeTab.builder()
                .title(CREATIVE_TAB_NAME_GUNS)
                .icon(() -> stacks.size() > 0 ? stacks.get(0) : new ItemStack(Items.DIAMOND))
                .displayItems((itemDisplayParameters, output) ->
                {
                    output.accept(GUN_MOD_TABLE_ITEM.get());
                    output.accept(GUN_MOD_PORTABLE_ITEM.get());
                    output.accept(DIESEL_GENERATOR_ITEM.get());
                    output.accept(COAL_GENERATOR_ITEM.get());
                    for(ItemStack stack : stacks)
                        output.accept(stack);
                })
                .build();
        });
    public static final Component CREATIVE_TAB_NAME_BULLETS = Component.translatable("item_group." + MODID + ".creative_tab_bullets");
    public static final Supplier<CreativeModeTab> CREATIVE_TAB_BULLETS = CREATIVE_TABS.register("creative_tab_bullets",
        () -> {
            List<ItemStack> stacks = new ArrayList<>();
            for(Item item : BuiltInRegistries.ITEM)
            {
                if(item instanceof BulletItem || item instanceof GrenadeItem)
                    stacks.add(new ItemStack(item));
            }

            return CreativeModeTab.builder()
                .title(CREATIVE_TAB_NAME_BULLETS)
                .icon(() -> stacks.size() > 0 ? stacks.get(0) : new ItemStack(Items.STICK))
                .displayItems((itemDisplayParameters, output) ->
                {
                    for(ItemStack stack : stacks)
                        output.accept(stack);
                })
                .build();
        });
    public static final Component CREATIVE_TAB_NAME_PARTS = Component.translatable("item_group." + MODID + ".creative_tab_parts");
    public static final Supplier<CreativeModeTab> CREATIVE_TAB_PARTS = CREATIVE_TABS.register("creative_tab_parts",
        () -> {
            List<ItemStack> stacks = new ArrayList<>();
            for(Item item : BuiltInRegistries.ITEM)
            {
                if(item instanceof PartItem)
                    stacks.add(new ItemStack(item));
            }

            return CreativeModeTab.builder()
                .title(CREATIVE_TAB_NAME_PARTS)
                .icon(() -> stacks.size() > 0 ? stacks.get(0) : new ItemStack(Items.STICK))
                .displayItems((itemDisplayParameters, output) ->
                {
                    for(ItemStack stack : stacks)
                        output.accept(stack);
                })
                .build();
        });
    public static final Component CREATIVE_TAB_NAME_MODIFIERS = Component.translatable("item_group." + MODID + ".creative_tab_modifiers");
    public static final Supplier<CreativeModeTab> CREATIVE_TAB_MODIFIERS = CREATIVE_TABS.register("creative_tab_modifiers",
        () -> {
            List<ItemStack> stacks = new ArrayList<>();
            for(Item item : BuiltInRegistries.ITEM)
            {
                if(item instanceof AttachmentItem)
                    stacks.add(new ItemStack(item));
            }

            return CreativeModeTab.builder()
                .title(CREATIVE_TAB_NAME_MODIFIERS)
                .icon(() -> stacks.size() > 0 ? stacks.get(0) : new ItemStack(Items.BOOK))
                .displayItems((itemDisplayParameters, output) ->
                {
                    output.accept(GUN_MOD_TABLE_ITEM.get());
                    output.accept(GUN_MOD_PORTABLE_ITEM.get());
                    output.accept(RAINBOW_PAINT_CAN_ITEM.get());
                    output.accept(MAG_UPGRADE_ITEM.get());
                    for(ItemStack stack : stacks)
                        output.accept(stack);
                })
                .build();
        });

    // Definition Repositories
    public static final GunDefinitions GUNS = new GunDefinitions();
    public static final BulletDefinitions BULLETS = new BulletDefinitions();
    public static final GrenadeDefinitions GRENADES = new GrenadeDefinitions();
    public static final AttachmentDefinitions ATTACHMENTS = new AttachmentDefinitions();
    public static final PartDefinitions PARTS = new PartDefinitions();
    public static final WorkbenchDefinitions WORKBENCHES = new WorkbenchDefinitions();
    public static final MaterialDefinitions MATERIALS = new MaterialDefinitions();
    public static final MagazineDefinitions MAGAZINES = new MagazineDefinitions();
    public static final NpcDefinitions NPCS = new NpcDefinitions();
    public static final CraftingTraitDefinitions TRAITS = new CraftingTraitDefinitions();
    public static final ArmourDefinitions ARMOURS = new ArmourDefinitions();
    public static final VehicleDefinitions VEHICLES = new VehicleDefinitions();
    public static final ControlSchemeDefinitions CONTROL_SCHEMES = new ControlSchemeDefinitions();

    // Server handlers
    public static final ServerActionManager ACTIONS_SERVER = new ServerActionManager();
    public static final ServerLongDistanceEntitySystem LONG_DISTANCE_SERVER = new ServerLongDistanceEntitySystem();
    public static final ContextCache CONTEXT_CACHE = new ServerContextCache();
    public static final ServerInventoryManager INVENTORY_MANAGER = new ServerInventoryManager();

    public static Supplier<Item> Gun(DeferredRegister<Item> itemRegister, String modID, String name)
    {
        ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(modID, name);
        return itemRegister.register(name, () -> new GunItem(loc, new Item.Properties().stacksTo(1)));
    }

    public static Supplier<Item> Tool(DeferredRegister<Item> itemRegister, String modID, String name)
    {
        // TODO: Check that this actually works correctly
        return Gun(itemRegister, modID, name);
    }


    public static Supplier<Item> Bullet(DeferredRegister<Item> itemRegister, String modID, String name)
    {
        ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(modID, name);
        return itemRegister.register(name, () -> new BulletItem(loc, new Item.Properties()));
    }

    public static Supplier<Item> Attachment(DeferredRegister<Item> itemRegister, String modID, String name)
    {
        ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(modID, name);
        return itemRegister.register(name, () -> new AttachmentItem(loc, new Item.Properties()));
    }

    public static Supplier<Item> Part(DeferredRegister<Item> itemRegister, String modID, String name)
    {
        ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(modID, name);
        return itemRegister.register(name, () -> new PartItem(loc, new Item.Properties()));
    }

    public static Supplier<Block> Workbench_Block(DeferredRegister<Block> blockRegister, String modID, String name)
    {
        ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(modID, name);
        return blockRegister.register(name, () -> new WorkbenchBlock(loc, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).dynamicShape()));
    }

    public static Supplier<Item> Workbench_Item(DeferredRegister<Item> itemRegister, String modID, String name, Supplier<Block> block)
    {
        ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(modID, name);
        return itemRegister.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }
    @Nonnull
    public static Supplier<Item> Workbench_Quick_Item(@Nonnull DeferredRegister<Item> itemRegister, @Nonnull String modID, @Nonnull String name)
    {
        ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(modID, name);
        return itemRegister.register(name, () -> new TemporaryWorkbenchItem(loc));
    }

    public static Supplier<BlockEntityType<WorkbenchBlockEntity>> Workbench_TileEntityType(DeferredRegister<BlockEntityType<?>> tileEntityTypeRegister, String modID, String name)
    {
        ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(modID, name);
        return tileEntityTypeRegister.register(name, () -> new WorkbenchBlockEntity.WorkbenchBlockEntityTypeHolder(loc).CreateType());
    }

    public static Supplier<Item> Vehicle_Item(DeferredRegister<Item> itemRegister, String modID, String name)
    {
        ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(modID, name);
        return itemRegister.register(name, () -> new VehicleItem(loc, new Item.Properties()));
    }


    private record VehicleFactory(@Nonnull ResourceLocation Loc)
    {
        @Nonnull
        public VehicleEntity CreateEntity(@Nonnull EntityType<VehicleEntity> type, @Nonnull Level level)
        {
            return new VehicleEntity(type, Loc, level);
        }
        @Nonnull
        public LongDistanceVehicle CreateLongDistance(@Nonnull EntityType<?> type)
        {
            return new LongDistanceVehicle(type, Loc);
        }
    }
    @Nonnull
    public static Supplier<EntityType<VehicleEntity>> Vehicle_Entity(
        @Nonnull DeferredRegister<EntityType<?>> entityRegister,
        @Nonnull String modID,
        @Nonnull String name,
        boolean longDistanceEnabled)
    {
        ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(modID, name);
        return entityRegister.register(name, () ->
        {
            VehicleFactory factory = new VehicleFactory(loc);
            EntityType<VehicleEntity> entityType = EntityType.Builder.of(factory::CreateEntity, MobCategory.MISC)
                .sized(1f, 1f)
                .build(name);
            if(longDistanceEnabled)
                LongDistanceEntitySystem.RegisterLongDistanceEntityClass(entityType, factory::CreateLongDistance);
            return entityType;
        });
    }

    @Nonnull
    public static ContextCache GetGunContextCache(boolean client)
    {
        if(client)
        {
            return GetClientGunContextCache();
        }
        return CONTEXT_CACHE;
    }
    @Nonnull
    private static ContextCache GetClientGunContextCache() { return FlansModClient.CONTEXT_CACHE; }

    public FlansMod()
    {
        ModLoadingContext.get().getActiveContainer().registerConfig(ModConfig.Type.COMMON, FlansModConfig.GeneralConfig, "flans-general.toml");

        NeoForge.EVENT_BUS.register(this);
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::CommonInit);
        ACTIONS_SERVER.HookServer(modEventBus);
        FlansModPacketHandler.RegisterMessages();
        FlansEntityDataSerializers.RegisterSerializers();
        modEventBus.addListener(this::OnRegsiterEvent);

        new NpcRelationshipCapabilityAttacher();

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        TILE_ENTITIES.register(modEventBus);
        MENUS.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        RECIPE_TYPES.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);
        LOOT_MODIFIERS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
        ATTRIBUTES.register(modEventBus);

        Transform.RunTests();
    }

    private void CommonInit(final FMLCommonSetupEvent event)
    {
    }

    @SubscribeEvent
    public void OnLevelLoad(@Nonnull LevelEvent.Load event)
    {
        if(!event.getLevel().isClientSide())
            new Raytracer(event.getLevel()).hook();
    }

    @SubscribeEvent
    public void OnLevelUnload(@Nonnull LevelEvent.Unload event)
    {
        if(!event.getLevel().isClientSide())
            CONTEXT_CACHE.OnLevelUnloaded(ACTIONS_SERVER);
    }
    @SubscribeEvent
    public void OnLevelTick(@Nonnull LevelTickEvent.Pre levelTick)
    {
        OBBCollisionSystem physics = OBBCollisionSystem.ForLevel(levelTick.getLevel());
        physics.PreTick();
    }
    @SubscribeEvent
    public void OnLevelTick(@Nonnull LevelTickEvent.Post levelTick)
    {
        OBBCollisionSystem physics = OBBCollisionSystem.ForLevel(levelTick.getLevel());
        physics.PhysicsTick();
    }

    @SubscribeEvent
    public void OnPlayerTravel(@Nonnull PlayerEvent.PlayerChangedDimensionEvent event)
    {
        CONTEXT_CACHE.ClearPlayer(event.getEntity().getUUID(), ACTIONS_SERVER);
    }

    private void OnRegsiterEvent(@Nonnull RegisterEvent event)
    {
        if(event.getRegistryKey().equals(NeoForgeRegistries.Keys.RECIPE_SERIALIZERS))
        {
            CraftingHelper.register(
                ResourceLocation.fromNamespaceAndPath(MODID, "tiered_material"),
                TieredMaterialIngredient.Serializer.INSTANCE);
            CraftingHelper.register(
                ResourceLocation.fromNamespaceAndPath(MODID, "stacked_vanilla"),
                StackedVanillaIngredient.Serializer.INSTANCE);
            CraftingHelper.register(
                ResourceLocation.fromNamespaceAndPath(MODID, "tiered_part"),
                TieredPartIngredient.Serializer.INSTANCE);
        }
    }

    @SubscribeEvent
    public void OnRegisterRecipeBookCategories(@Nonnull RegisterRecipeBookCategoriesEvent event)
    {
        
    }



    @SubscribeEvent
    public void OnReloadResources(AddReloadListenerEvent event)
    {
        if(FMLEnvironment.dist == Dist.DEDICATED_SERVER)
        {
            RegisterCommonReloadListeners(event::addListener);
        }
    }

    @SubscribeEvent
    public void OnLivingDeath(LivingDeathEvent deathEvent)
    {
        DamageSource damageSource = deathEvent.getSource();
        LivingEntity target = deathEvent.getEntity();
        if(!damageSource.is(DamageTypeTags.BYPASSES_INVULNERABILITY))
        {
            if(target instanceof ServerPlayer player)
            {
                ShooterContext shooterContext = CONTEXT_CACHE.GetShooter(player);
                for(GunContext gunContext : shooterContext.GetAllGunContexts(false))
                {
                    if(gunContext.IsValid() && gunContext instanceof GunContextPlayer gunContextPlayer)
                    {
                        // We ONLY check for this ability on attachments, because we need something to consume to stop it going infinite
                        for(EAttachmentType attachmentType : EAttachmentType.values())
                        {
                            for(int i = 0; i < gunContext.GetNumAttachmentStacks(attachmentType); i++)
                            {
                                ItemStack attachmentStack = gunContext.GetAttachmentStack(attachmentType, i);
                                if(attachmentStack.getItem() instanceof AttachmentItem attachmentItem)
                                {
                                    for(CraftingTraitProviderDefinition abilityProvider : attachmentItem.Def().abilities)
                                    {
                                        CraftingTraitDefinition traitDef = abilityProvider.GetAbility();
                                        for(AbilityDefinition abilityDef : traitDef.abilities)
                                        {
                                            for (AbilityEffectDefinition effectDef : abilityDef.effects)
                                            {
                                                if (effectDef.effectType == EAbilityEffect.TotemOfUndying)
                                                {
                                                    if (CommonHooks.onLivingUseTotem(player, damageSource, attachmentStack, gunContextPlayer.GetHand()))
                                                    {
                                                        gunContext.SetAttachmentStack(attachmentType, i, ItemStack.EMPTY);

                                                        player.awardStat(Stats.ITEM_USED.get(attachmentStack.getItem()), 1);
                                                        CriteriaTriggers.USED_TOTEM.trigger(player, attachmentStack);

                                                        player.setHealth(1.0f);
                                                        player.removeAllEffects();
                                                        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
                                                        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
                                                        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
                                                        player.level().broadcastEntityEvent(player, (byte) 35);

                                                        deathEvent.setCanceled(true);
                                                        return;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static void RegisterCommonReloadListeners(Consumer<PreparableReloadListener> registerFunc)
    {
        registerFunc.accept(GUNS);
        registerFunc.accept(BULLETS);
        registerFunc.accept(ATTACHMENTS);
        registerFunc.accept(PARTS);
        registerFunc.accept(WORKBENCHES);
        registerFunc.accept(MATERIALS);
        registerFunc.accept(MAGAZINES);
        registerFunc.accept(NPCS);
        registerFunc.accept(TRAITS);
        registerFunc.accept(ARMOURS);
        registerFunc.accept(VEHICLES);
        registerFunc.accept(CONTROL_SCHEMES);
    }
}
