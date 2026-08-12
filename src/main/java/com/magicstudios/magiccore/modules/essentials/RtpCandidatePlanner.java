package com.magicstudios.magiccore.modules.essentials;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

/** Generates bounded candidates only; terrain/protection checks must run on each candidate's region thread. */
public final class RtpCandidatePlanner {
    public List<Candidate> plan(RtpBounds bounds, RandomGenerator random) {
        List<Candidate> result = new ArrayList<>(bounds.maximumAttempts());
        double minimumSquared = bounds.minimumRadius() * bounds.minimumRadius();
        double maximumSquared = bounds.maximumRadius() * bounds.maximumRadius();
        for (int i = 0; i < bounds.maximumAttempts(); i++) {
            double angle = random.nextDouble(0, Math.PI * 2);
            double radius = Math.sqrt(random.nextDouble(minimumSquared, maximumSquared));
            result.add(new Candidate(bounds.centerX() + Math.cos(angle) * radius,
                    bounds.centerZ() + Math.sin(angle) * radius));
        }
        return List.copyOf(result);
    }

    public record Candidate(double x, double z) { }
}
