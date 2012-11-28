package hudson.plugins.erlangcover.targets;

import hudson.plugins.erlangcover.Ratio;

import java.io.Serializable;
import java.util.EnumMap;
import java.util.Map;

import static hudson.plugins.erlangcover.targets.CoverageAggregationMode.COUNT_NON_ZERO;
import static hudson.plugins.erlangcover.targets.CoverageAggregationMode.SUM;
import static hudson.plugins.erlangcover.targets.CoverageElement.*;
import static hudson.plugins.erlangcover.targets.CoverageMetric.*;

/**
 * Rules that determines how coverage ratio of children are aggregated into that of the parent.
 *
 *
 * @author Stephen Connolly
 * @since 22-Aug-2007 18:08:46
 */
public class CoverageAggregationRule implements Serializable {
    private static final long serialVersionUID = 3610276359557022488L;
    private final CoverageElement source;
    private final CoverageMetric input;
    private final CoverageAggregationMode mode;
    private final CoverageMetric output;

    public CoverageAggregationRule(CoverageElement source,
                                   CoverageMetric input,
                                   CoverageAggregationMode mode,
                                   CoverageMetric output) {
        this.mode = mode;
        this.input = input;
        this.source = source;
        this.output = output;
    }

    public static Map<CoverageMetric, Ratio> aggregate(CoverageElement source,
                                                       CoverageMetric input,
                                                       Ratio inputResult,
                                                       Map<CoverageMetric, Ratio> runningTotal) {
        Map<CoverageMetric, Ratio> result = new EnumMap<CoverageMetric,Ratio>(CoverageMetric.class);
        result.putAll(runningTotal);
        for (CoverageAggregationRule rule : INITIAL_RULESET) {
            if (rule.source==source && rule.input==input) {
                Ratio prevTotal = result.get(rule.output);
                if (prevTotal==null)    prevTotal = rule.mode.ZERO;

                result.put(rule.output, rule.mode.aggregate(prevTotal,inputResult));
            }
        }
        return result;
    }

    // read (a,b,c,d) as "b metric of a is aggregated into d metric of the parent by using method c."
    // for example, line coverage of a Java method is SUMed up to the line coverage of a Java class (its parent) (1st line),
    // the method coverage of a Java class is # of methods that have some coverage among # of methods that have any code (3rd line.)
    // and so on.
    private static final CoverageAggregationRule INITIAL_RULESET[] = {
            new CoverageAggregationRule(ERLANG_FUNCTION, LINE,   SUM, LINE),
            new CoverageAggregationRule(ERLANG_FUNCTION, LINE,   COUNT_NON_ZERO, FUNCTION),

            new CoverageAggregationRule(ERLANG_MODULE, LINE,     SUM, LINE),
            new CoverageAggregationRule(ERLANG_MODULE, FUNCTION, SUM, FUNCTION),
            new CoverageAggregationRule(ERLANG_MODULE, LINE,     COUNT_NON_ZERO, MODULES),
    };

    public static Ratio combine(CoverageMetric metric,
                                Ratio existingResult, Ratio additionalResult) {
        return Ratio.create(existingResult.numerator + additionalResult.numerator, existingResult.denominator + additionalResult.denominator);
    }
}
