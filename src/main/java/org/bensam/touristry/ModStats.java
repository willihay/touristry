package org.bensam.touristry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.stats.StatFormatter;
import net.minecraft.stats.Stats;

public class ModStats {
    private ModStats() {}

    //private static Identifier wandsEnchanted;

    public static void initialize() {
        // wandsEnchanted = makeCustomStat("wands_enchanted", StatFormatter.DEFAULT);
    }

//    public static Stat<Identifier> getWandsEnchantedStat() {
//        return Stats.CUSTOM.get(wandsEnchanted);
//    }

    private static Identifier makeCustomStat(String name, StatFormatter statFormatter) {
        Identifier id = Identifier.fromNamespaceAndPath(Touristry.MOD_ID, name);
        Registry.register(BuiltInRegistries.CUSTOM_STAT, id.getPath(), id);
        Stats.CUSTOM.get(id, statFormatter);
        return id;
    }
}
