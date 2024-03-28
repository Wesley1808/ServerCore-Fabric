package me.wesley1808.servercore.common.config.files.data.activation_range;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import me.wesley1808.servercore.common.activation_range.EntityTypeTests;
import me.wesley1808.servercore.common.config.serialization.EntityTypeSerializer;
import me.wesley1808.servercore.common.config.serialization.EntityTypeTestSerializer;
import net.minecraft.world.entity.EntityType;
import space.arim.dazzleconf.annote.ConfComments;
import space.arim.dazzleconf.annote.ConfDefault.DefaultBoolean;
import space.arim.dazzleconf.annote.ConfDefault.DefaultInteger;
import space.arim.dazzleconf.annote.ConfDefault.DefaultObject;
import space.arim.dazzleconf.annote.ConfKey;
import space.arim.dazzleconf.annote.ConfSerialisers;
import space.arim.dazzleconf.annote.SubSection;
import space.arim.dazzleconf.sorter.AnnotationBasedSorter.Order;

import java.util.List;
import java.util.Set;

@ConfSerialisers({EntityTypeSerializer.class, EntityTypeTestSerializer.class})
public interface ActivationRangeConfig {
    @Order(1)
    @ConfKey("enabled")
    @DefaultBoolean(false)
    @ConfComments("(Default = false) Enables this feature.")
    boolean enabled();

    @Order(2)
    @ConfKey("tick-new-entities")
    @DefaultBoolean(true)
    @ConfComments({
            "(Default = true) Briefly ticks entities newly added to the world for 10 seconds (includes both spawning and loading).",
            "This gives them a chance to properly immunize when they are spawned if they should be. Can be helpful for mobfarms."
    })
    boolean tickNewEntities();

    @Order(3)
    @ConfKey("use-vertical-range")
    @DefaultBoolean(false)
    @ConfComments({
            "(Default = false) Enables vertical range checks. By default, activation ranges only work horizontally.",
            "This can greatly improve performance on taller worlds, but might break a few very specific ai-based mobfarms."
    })
    boolean useVerticalRange();

    @Order(4)
    @ConfKey("skip-non-immune")
    @DefaultBoolean(false)
    @ConfComments({
            "(Default = false) Skips 1/4th of entity ticks whilst not immune.",
            "This affects entities that are within the activation range, but not immune (for example by falling or being in water)."
    })
    boolean skipNonImmune();

    @Order(5)
    @ConfKey("villager-tick-panic")
    @DefaultBoolean(true)
    @ConfComments("Allows villagers to tick regardless of the activation range when panicking.")
    boolean villagerTickPanic();

    @Order(6)
    @ConfKey("villager-work-immunity-after")
    @DefaultInteger(20)
    @ConfComments("The time in seconds that a villager needs to be inactive for before obtaining work immunity (if it has work tasks).")
    int villagerWorkImmunityAfter();

    @Order(7)
    @ConfKey("villager-work-immunity-for")
    @DefaultInteger(20)
    @ConfComments("The amount of ticks an inactive villager will wake up for when it has work immunity.")
    int villagerWorkImmunityFor();

    @Order(8)
    @ConfKey("excluded-entity-types")
    @DefaultObject("defaultExcludedEntityTypes")
    @ConfComments({
            "(Default = [minecraft:hopper_minecart, minecraft:warden, minecraft:ghast])",
            "A list of entity types that should be excluded from activation range checks."
    })
    Set<EntityType<?>> excludedEntityTypes();

    @Order(9)
    @SubSection
    @ConfKey("default-activation-type")
    @ConfComments({
            "The activation type that will get assigned to any entity that doesn't have a custom activation type.",
            "► activation-range = The range an entity is required to be in from a player to be activated.",
            "► tick-interval = The interval between 'active' ticks whilst the entity is inactive. Negative values will disable these active ticks.",
            "► wakeup-interval = The interval between inactive entity wakeups in seconds.",
            "► extra-height-up = Allows entities to be ticked when far above the player when vertical range is in use.",
            "► extra-height-down = Allows entities to be ticked when far below the player when vertical range is in use."
    })
    ActivationType defaultActivationType();

    @Order(10)
    @ConfKey("custom-activation-types")
    @DefaultObject("defaultActivationTypes")
    @ConfComments({
            "A list of custom activation types.",
            "► name = The name of the activation type.",
            "► entity-matcher = A list of conditions to filter entities. Only one of these conditions needs to be met for an entity to match.",
            "► If an entity matches multiple activation types, the one highest in the list will be used. The conditions accept the following formats:",
            "  - Entity type matching    |   Uses the entity type's registry key.  |  'minecraft:zombie' matches zombies, but for example not husks or drowned.",
            "  - Typeof class matching   |   Uses the 'typeof:' prefix.            |  'typeof:monster' matches all monsters.",
            "► Available typeof classes: mob, monster, raider, neutral, ambient, animal, water_animal, flying_animal, flying_monster, villager."
    })
    List<@SubSection CustomActivationType> activationTypes();

    static Set<EntityType<?>> defaultExcludedEntityTypes() {
        return Sets.newHashSet(
                EntityType.HOPPER_MINECART,
                EntityType.WARDEN,
                EntityType.GHAST
        );
    }

    static List<@SubSection CustomActivationType> defaultActivationTypes() {
        return Lists.newArrayList(
                CustomActivationType.of("raider",
                        Lists.newArrayList(EntityTypeTests.RAIDER),
                        48, 20, 20, true, false
                ),
                CustomActivationType.of("water",
                        Lists.newArrayList(EntityTypeTests.WATER_ANIMAL),
                        16, 20, 60, false, false
                ),
                CustomActivationType.of("villager",
                        Lists.newArrayList(EntityTypeTests.VILLAGER),
                        16, 20, 30, false, false
                ),
                CustomActivationType.of("zombie",
                        Lists.newArrayList(EntityType.ZOMBIE, EntityType.HUSK),
                        16, 20, 20, true, false
                ),
                CustomActivationType.of("monster-below",
                        Lists.newArrayList(EntityType.CREEPER, EntityType.SLIME, EntityType.MAGMA_CUBE, EntityType.HOGLIN),
                        32, 20, 20, true, true
                ),
                CustomActivationType.of("flying-monster",
                        Lists.newArrayList(EntityType.GHAST, EntityType.PHANTOM),
                        48, 20, 20, true, false
                ),
                CustomActivationType.of("monster",
                        Lists.newArrayList(EntityTypeTests.MONSTER),
                        32, 20, 20, true, false
                ),
                CustomActivationType.of("animal",
                        Lists.newArrayList(EntityTypeTests.ANIMAL, EntityTypeTests.AMBIENT),
                        16, 20, 60, false, false
                ),
                CustomActivationType.of("creature",
                        Lists.newArrayList(EntityTypeTests.MOB),
                        24, 20, 30, false, false
                )
        );
    }
}
