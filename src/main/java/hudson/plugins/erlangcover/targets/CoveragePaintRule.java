package hudson.plugins.erlangcover.targets;

import java.io.Serializable;

/**
 * Describes how {@link CoveragePaint} can be aggregated up a {@link CoverageResult} tree.
 *
 * @author Stephen Connolly
 * @since 29-Aug-2007 18:13:22
 */
public class CoveragePaintRule implements Serializable {
    private static final long serialVersionUID = 1039455096344866574L;
    private final CoverageElement element;
    private final CoverageAggregationMode mode;

    public CoveragePaintRule(CoverageElement element, CoverageAggregationMode mode) {
        this.element = element;
        this.mode = mode;
    }

    private static final CoveragePaintRule[] INITIAL_RULESET = {
            new CoveragePaintRule(CoverageElement.ERLANG_FUNCTION, CoverageAggregationMode.SUM),
            new CoveragePaintRule(CoverageElement.ERLANG_MODULE, CoverageAggregationMode.SUM),
    };

    public static CoveragePaint makePaint(CoverageElement element) {
        for (CoveragePaintRule rule : INITIAL_RULESET) {
            if (element==rule.element
                    || (element==rule.element.getParent() && CoverageAggregationMode.NONE!=rule.mode)) {
                return new CoveragePaint();
            }
        }
        return null;
    }

    public static boolean propagatePaintToParent(CoverageElement element) {
        for (CoveragePaintRule rule : INITIAL_RULESET) {
            if (element==rule.element) {
                return CoverageAggregationMode.NONE!=rule.mode;
            }
        }
        return false;
    }
}
