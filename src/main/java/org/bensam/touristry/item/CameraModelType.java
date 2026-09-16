package org.bensam.touristry.item;

import com.mojang.serialization.Codec;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.Util;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.CustomModelData;
import org.bensam.touristry.ModItems;
import org.bensam.touristry.ModSounds;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

public enum CameraModelType implements StringRepresentable {
    BLOCK(ModItems.TOURIST_CAMERA, ModSounds.CAMERA, ModSounds.CAMERA_FLASH, List.of(), List.of()),
    INSTAMATIC(ModItems.TOURIST_CAMERA_INSTAMATIC, ModSounds.CAMERA_INSTAMATIC, ModSounds.CAMERA_INSTAMATIC_FLASH, List.of("base"), List.of("cube", "extender")),
    PHONE_CAMERA(ModItems.TOURIST_PHONE, ModSounds.CAMERA_CELLPHONE, ModSounds.CAMERA_CELLPHONE_FLASH, List.of("base", "selfie_stick"), List.of("base"));

    public static final Codec<CameraModelType> CODEC = StringRepresentable.fromEnum(CameraModelType::values);

    private final Supplier<Item> item;
    private final SoundEvent baseSound;
    private final SoundEvent withFlashSound;
    private final List<String> baseModelVariants;
    private final List<String> flashVariants;

    CameraModelType(Supplier<Item> item, SoundEvent baseSound, SoundEvent withFlashSound, List<String> baseModelVariants, List<String> flashVariants) {
        this.item = item;
        this.baseSound = baseSound;
        this.withFlashSound = withFlashSound;
        this.baseModelVariants = baseModelVariants;
        this.flashVariants = flashVariants;
    }

    public static CameraModelType getRandom(RandomSource random) {
        return Util.getRandom(values(), random);
    }

    public static boolean isModelOnSelfieStick(@Nullable CustomModelData data) {
        return data != null && data.strings().contains("selfie_stick");
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Item getItem() {
        return this.item.get();
    }

    public SoundEvent getBaseSound() {
        return this.baseSound;
    }

    public SoundEvent getWithFlashSound() {
        return this.withFlashSound;
    }

    public CustomModelData getRandomBaseModelData(RandomSource random) {
        String variant = Util.getRandom(this.baseModelVariants, random);
        return new CustomModelData(List.of(), List.of(), List.of(variant), List.of());
    }

    public CustomModelData getRandomFlashModelData(RandomSource random) {
        String variant = Util.getRandom(this.flashVariants, random);
        return new CustomModelData(List.of(), List.of(), List.of(variant), List.of());
    }

    public boolean hasBaseModelVariants() {
        return !this.baseModelVariants.isEmpty();
    }

    public boolean hasFlashModelVariants() {
        return !this.flashVariants.isEmpty();
    }
}
