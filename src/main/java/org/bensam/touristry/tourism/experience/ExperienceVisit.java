package org.bensam.touristry.tourism.experience;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

import java.util.List;
import java.util.UUID;

public record ExperienceVisit(
        UUID experienceUUID,
        List<ExperienceTarget> remainingTargets,
        int targetsCompleted
) {
    public static final Codec<ExperienceVisit> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("experience_uuid").forGetter(ExperienceVisit::experienceUUID),
            ExperienceTarget.CODEC.listOf().fieldOf("remaining_targets").forGetter(ExperienceVisit::remainingTargets),
            Codec.INT.fieldOf("targets_completed").forGetter(ExperienceVisit::targetsCompleted)
    ).apply(instance, ExperienceVisit::new));
}
