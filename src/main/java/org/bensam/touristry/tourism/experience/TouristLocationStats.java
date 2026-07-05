package org.bensam.touristry.tourism.experience;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class TouristLocationStats {
    private int totalVisits;
    private int completedVisits;
    private int abandonedVisits;
    private int failedSpawns;
    private int closedEarly;
    private int navFailures;
    private int touristsHurt;
    private int touristsKilled;
    private long lastVisitTime;
    private double reputationScore;

    public static final Codec<TouristLocationStats> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("total_visits").forGetter(stats -> stats.totalVisits),
            Codec.INT.fieldOf("completed_visits").forGetter(stats -> stats.completedVisits),
            Codec.INT.fieldOf("abandoned_visits").forGetter(stats -> stats.abandonedVisits),
            Codec.INT.fieldOf("failed_spawns").forGetter(stats -> stats.failedSpawns),
            Codec.INT.fieldOf("closed_early").forGetter(stats -> stats.closedEarly),
            Codec.INT.fieldOf("nav_failures").forGetter(stats -> stats.navFailures),
            Codec.INT.fieldOf("tourists_hurt").forGetter(stats -> stats.touristsHurt),
            Codec.INT.fieldOf("tourists_killed").forGetter(stats -> stats.touristsKilled),
            Codec.LONG.fieldOf("last_visit_time").forGetter(stats -> stats.lastVisitTime),
            Codec.DOUBLE.fieldOf("reputation_score").forGetter(stats -> stats.reputationScore)
    ).apply(instance, TouristLocationStats::new));

    public TouristLocationStats() {
        this(0, 0, 0, 0, 0, 0, 0, 0, 0L, 0.0d);
    }

    public TouristLocationStats(int totalVisits, int completedVisits, int abandonedVisits,
                                int failedSpawns, int closedEarly, int navFailures,
                                int touristsHurt, int touristsKilled,
                                long lastVisitTime, double reputationScore) {
        this.totalVisits = totalVisits;
        this.completedVisits = completedVisits;
        this.abandonedVisits = abandonedVisits;
        this.failedSpawns = failedSpawns;
        this.closedEarly = closedEarly;
        this.navFailures = navFailures;
        this.touristsHurt = touristsHurt;
        this.touristsKilled = touristsKilled;
        this.lastVisitTime = lastVisitTime;
        this.reputationScore = reputationScore;
    }

    // Getters
    public int getTotalVisits() { return this.totalVisits; }
    public int getCompletedVisits() { return this.completedVisits; }
    public int getAbandonedVisits() { return this.abandonedVisits; }
    public int getFailedSpawns() { return this.failedSpawns; }
    public int getClosedEarly() { return this.closedEarly; }
    public int getNavFailures() { return this.navFailures; }
    public int getTouristsHurt() { return this.touristsHurt; }
    public int getTouristsKilled() { return this.touristsKilled; }
    public long getLastVisitTime() { return this.lastVisitTime; }
    public double getReputation() { return this.reputationScore; }

    public void recordVisit(long visitTime) {
        this.totalVisits++;
        this.lastVisitTime = visitTime;
    }

    public void recordCompletedVisit() {
        this.completedVisits++;
    }

    public void recordAbandonedVisit() {
        this.abandonedVisits++;
    }

    public void recordClosedEarly() {
        this.closedEarly++;
    }

    public void recordFailedSpawn() {
        this.failedSpawns++;
    }

    public void recordNavFailure() {
        this.navFailures++;
    }

    public void recordTouristHurt() {
        this.touristsHurt++;
    }

    public void recordTouristKilled() {
        this.touristsKilled++;
    }

    public void resetAll() {
        this.totalVisits = 0;
        this.completedVisits = 0;
        this.abandonedVisits = 0;
        this.failedSpawns = 0;
        this.closedEarly = 0;
        this.navFailures = 0;
        this.touristsHurt = 0;
        this.touristsKilled = 0;
        this.lastVisitTime = 0L;
        this.reputationScore = 0.0d;
    }

    public void resetReputation() {
        this.reputationScore = 0.0d;
    }

    public void setReputation(double reputationScore) {
        this.reputationScore = reputationScore;
    }
}
