package org.bensam.touristry.tourism.experience;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class ExperienceStatistics {
    private int totalVisits;
    private int completedVisits;
    private int abandonedVisits;
    private long lastVisitTime;
    private double reputationScore;

    public static final Codec<ExperienceStatistics> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("total_visits").forGetter(stats -> stats.totalVisits),
            Codec.INT.fieldOf("completed_visits").forGetter(stats -> stats.completedVisits),
            Codec.INT.fieldOf("abandoned_visits").forGetter(stats -> stats.abandonedVisits),
            Codec.LONG.fieldOf("last_visit_time").forGetter(stats -> stats.lastVisitTime),
            Codec.DOUBLE.fieldOf("reputation_score").forGetter(stats -> stats.reputationScore)
    ).apply(instance, ExperienceStatistics::new));

    public ExperienceStatistics() {
        this(0, 0, 0, 0L, 0.0d);
    }

    public ExperienceStatistics(int totalVisits, int completedVisits,
                                int abandonedVisits, long lastVisitTime,
                                double reputationScore) {
        this.totalVisits = totalVisits;
        this.completedVisits = completedVisits;
        this.abandonedVisits = abandonedVisits;
        this.lastVisitTime = lastVisitTime;
        this.reputationScore = reputationScore;
    }

    // Getters
    public int getTotalVisits() { return totalVisits; }
    public int getCompletedVisits() { return completedVisits; }
    public int getAbandonedVisits() { return abandonedVisits; }
    public long getLastVisitTime() { return lastVisitTime; }
    public double getReputationScore() { return reputationScore; }

    public void recordVisit(long visitTime) {
        this.totalVisits++;
        this.lastVisitTime = visitTime;
    }

    public void recordCompletedVisit(double reputationChange) {
        this.completedVisits++;
        this.reputationScore += reputationChange;
    }

    public void recordAbandonedVisit(double reputationChange) {
        this.abandonedVisits++;
        this.reputationScore += reputationChange;
    }

    public void resetAll() {
        this.totalVisits = 0;
        this.completedVisits = 0;
        this.abandonedVisits = 0;
        this.lastVisitTime = 0L;
        this.reputationScore = 0.0d;
    }

    public void resetReputation() {
        this.reputationScore = 0.0d;
    }
}
