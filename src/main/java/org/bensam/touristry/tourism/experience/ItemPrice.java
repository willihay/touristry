package org.bensam.touristry.tourism.experience;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.*;

// Implementation note: A null cost means "use default price" set in the experience block.
public record ItemPrice (ItemStack itemForSale, @Nullable ItemStack cost) implements Comparable<ItemPrice> {
    public static final Codec<ItemPrice> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemStack.CODEC.fieldOf("item_for_sale").forGetter(ItemPrice::itemForSale),
            ItemStack.OPTIONAL_CODEC.optionalFieldOf("cost").forGetter(itemPrice -> Optional.ofNullable(itemPrice.cost()))
    ).apply(instance, (itemForSale, cost) -> new ItemPrice(itemForSale, cost.orElse(null))));

    public static final Codec<TreeSet<ItemPrice>> TREESET_CODEC =
            ItemPrice.CODEC.listOf().xmap(
                    list -> {
                        TreeSet<ItemPrice> set = new TreeSet<>();
                        set.addAll(list); // uses compareTo()
                        return set;
                    },
                    ArrayList::new
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, ItemPrice> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC, ItemPrice::itemForSale,
            ByteBufCodecs.optional(ItemStack.OPTIONAL_STREAM_CODEC), itemPrice -> Optional.ofNullable(itemPrice.cost()),
            (itemForSale, cost) -> new ItemPrice(itemForSale, cost.orElse(null))
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, TreeSet<ItemPrice>> TREESET_STREAM_CODEC = StreamCodec.of(
            (buf, set) -> {
                // encode as a list
                List<ItemPrice> list = new ArrayList<>(set);
                ItemPrice.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, list);
            },
            buf -> {
                // decode
                List<ItemPrice> list = ItemPrice.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf);
                TreeSet<ItemPrice> set = new TreeSet<>();
                set.addAll(list); // uses compareTo()
                return set;
            }
    );

    @Override
    public int compareTo(@NonNull ItemPrice other) {
        String strThis = this.itemForSale.getHoverName().getString().toLowerCase(Locale.ROOT);
        String strOther = other.itemForSale.getHoverName().getString().toLowerCase(Locale.ROOT);

        int cmp = strThis.compareTo(strOther);
        if (cmp != 0) {
            return cmp;
        }

        // tie-breaker: compare item registry names
        return BuiltInRegistries.ITEM.getKey(this.itemForSale.getItem())
                .compareTo(BuiltInRegistries.ITEM.getKey(other.itemForSale.getItem()));
    }
}
