package org.bensam.touristry.tourism.experience;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import org.bensam.touristry.block.entity.ItemStackKey;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Optional;

// Implementation note: A null cost means "use default price" set in the experience block.
public record ItemPrice (ItemStack itemForSale, @Nullable ItemStack cost) {
    public static final Codec<ItemPrice> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemStack.CODEC.fieldOf("item_for_sale").forGetter(ItemPrice::itemForSale),
            ItemStack.OPTIONAL_CODEC.optionalFieldOf("cost").forGetter(itemPrice -> Optional.ofNullable(itemPrice.cost()))
    ).apply(instance, (itemForSale, cost) -> new ItemPrice(itemForSale, cost.orElse(null))));

    public static final Codec<LinkedHashMap<ItemStackKey, ItemPrice>> MAP_CODEC =
            ItemPrice.CODEC.listOf().xmap(
                    list -> {
                        LinkedHashMap<ItemStackKey, ItemPrice> map = new LinkedHashMap<>();
                        for (ItemPrice itemPrice : list) {
                            map.put(new ItemStackKey(itemPrice.itemForSale()), itemPrice);
                        }
                        return map;
                    },
                    map -> map.values().stream()
                            .sorted(ItemPrice.DISPLAY_ORDER)
                            .toList()
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, ItemPrice> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC, ItemPrice::itemForSale,
            ByteBufCodecs.optional(ItemStack.OPTIONAL_STREAM_CODEC), itemPrice -> Optional.ofNullable(itemPrice.cost()),
            (itemForSale, cost) -> new ItemPrice(itemForSale, cost.orElse(null))
    );

    public static final Comparator<ItemPrice> DISPLAY_ORDER = Comparator.comparing(
            (ItemPrice itemPrice) -> itemPrice.itemForSale().getHoverName().getString(),
            String.CASE_INSENSITIVE_ORDER
    ).thenComparing(
            itemPrice -> BuiltInRegistries.ITEM.getKey(itemPrice.itemForSale().getItem())
    ).thenComparingInt(
            itemPrice -> ItemStack.hashItemAndComponents(itemPrice.itemForSale())
    );
}
