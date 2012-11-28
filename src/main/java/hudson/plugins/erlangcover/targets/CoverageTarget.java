package hudson.plugins.erlangcover.targets;

import hudson.plugins.erlangcover.Ratio;

import java.io.Serializable;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Holds the target coverage for a specific condition;
 *
 * @author Stephen Connolly
 * @since 1.1
 */
public class CoverageTarget implements Serializable {

    private static final long serialVersionUID = -1230271515322670492L;
    
    private Map<CoverageMetric, Float> targets = new EnumMap<CoverageMetric, Float>(CoverageMetric.class);

    /**
     * Constructs a new CoverageTarget.
     */
    public CoverageTarget() {
    }

    public CoverageTarget(Map<CoverageMetric, Float> coverage) {
        this.targets.putAll(coverage);
    }

    /**
     * Getter for property 'alwaysMet'.
     *
     * @return Value for property 'alwaysMet'.
     */
    public boolean isAlwaysMet() {
        for (Float target : targets.values()) {
            if (target != null && target > 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Getter for property 'empty'.
     *
     * @return Value for property 'empty'.
     */
    public boolean isEmpty() {
        for (Float target : targets.values()) {
            if (target != null) {
                return false;
            }
        }
        return true;
    }

    public Set<CoverageMetric> getFailingMetrics(CoverageResult coverage) {
        Set<CoverageMetric> result = EnumSet.noneOf(CoverageMetric.class);
        for (Map.Entry<CoverageMetric, Float> target : this.targets.entrySet()) {
            Ratio observed = coverage.getCoverage(target.getKey());
            if (observed != null &&
                    roundFloatDecimal(observed.getPercentageFloat()) < target.getValue()) {
                result.add(target.getKey());
            }
        }

        return result;
    }

    public Set<CoverageMetric> getAllMetrics(CoverageResult coverage) {
        Set<CoverageMetric> result = EnumSet.noneOf(CoverageMetric.class);
        for (Map.Entry<CoverageMetric, Float> target : this.targets.entrySet()) {
            Ratio observed = coverage.getCoverage(target.getKey());
            if (observed != null) {
                result.add(target.getKey());
            }
        }

        return result;
    }
    
    public float getObservedPercent(CoverageResult coverage, CoverageMetric key)
    {
        for (Map.Entry<CoverageMetric, Float> target : this.targets.entrySet()) {
            Ratio observed = coverage.getCoverage(target.getKey());
            if (target.getKey() == key) {
            	return roundFloatDecimal(observed.getPercentageFloat());
            }
        }
        return 0;
    }
    
    public float getSetPercent(CoverageResult coverage, CoverageMetric key)
    {
        for (Map.Entry<CoverageMetric, Float> target : this.targets.entrySet()) {
            if (target.getKey() == key) {
            	return target.getValue();
            }
        }
        return 0;
    }
    
    public Map<CoverageMetric, Float> getRangeScores(CoverageTarget min, CoverageResult coverage) {
        return getRangeScores(min, coverage.getResults());
    }

    public Map<CoverageMetric, Float> getRangeScores(CoverageTarget min, Map<CoverageMetric, Ratio> results) {
        Map<CoverageMetric, Float> result = new EnumMap<CoverageMetric, Float>(CoverageMetric.class);
        for (Map.Entry<CoverageMetric, Float> target : this.targets.entrySet()) {
            Ratio observed = results.get(target.getKey());
            if (observed != null) {
                float j = CoverageTarget.calcRangeScore(target.getValue(),
                        min.targets.get(target.getKey()), observed.getPercentage());
                result.put(target.getKey(), j);
            }
        }
        return result;
    }

    private static float calcRangeScore(Float max, Float min, int value) {
        if (min == null || min < 0) min = 0.0f;
        if (max == null || max > 100) max = 100.0f;
        if (min > max) min = max - 1;
        int result = (int) (100f * (value - min.floatValue()) / (max.floatValue() - min.floatValue()));
        if (result < 0) return 0;
        if (result > 100) return 100;
        return result;
    }

    /**
     * Getter for property 'targets'.
     *
     * @return Value for property 'targets'.
     */
    public Set<CoverageMetric> getTargets() {
        Set<CoverageMetric> targets = EnumSet.noneOf(CoverageMetric.class);
        for (Map.Entry<CoverageMetric, Float> target : this.targets.entrySet()) {
            if (target.getValue() != null) {
                targets.add(target.getKey());
            }
        }
        return targets;
    }

    public void setTarget(CoverageMetric metric, Float target) {
        targets.put(metric, target);
    }

    public Float getTarget(CoverageMetric metric) {
        return targets.get(metric);
    }

    public void clear() {
        targets.clear();
    }
    
    public float roundFloatDecimal(float input) {
    	float rounded = (float)Math.round(input*100f);
    	rounded = rounded / 100f;
    	return rounded;
    }
}
