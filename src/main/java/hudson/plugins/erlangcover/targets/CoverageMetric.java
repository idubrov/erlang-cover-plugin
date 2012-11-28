package hudson.plugins.erlangcover.targets;

/**
 * @author connollys
 * @author manolo
 * @since 10-Jul-2007 14:59:50
 */
public enum CoverageMetric {
    MODULES {
        public String getDisplayName() {
            return Messages.CoverageMetrics_Modules();
        }
    },
    FUNCTION {
        public String getDisplayName() {
            return Messages.CoverageMetrics_Functions();
        }
    },
    LINE {
        public String getDisplayName() {
            return Messages.CoverageMetrics_Lines();
        }
    };

    /**
     * Return the name of this metric element.
     * <p/>
     * Note: This getter has to be evaluated each time in a non static
     * way because the user could change its language
     *
     * @return Value for property 'displayName'.
     */
    public abstract String getDisplayName();
}

