package hudson.plugins.erlangcover.targets;

/**
 * Type of program construct being covered.
 *
 * @author Stephen Connolly
 * @author manolo
 * @since 22-Aug-2007 18:36:01
 */
public enum CoverageElement {

    PROJECT {
        public String getDisplayName() {
            return Messages.CoverageElement_Project();
        }
    },
    ERLANG_MODULE(PROJECT) {
        public String getDisplayName() {
            return Messages.CoverageElement_Module();
        }
    },
    ERLANG_FUNCTION(ERLANG_MODULE) {
        public String getDisplayName() {
            return Messages.CoverageElement_Function();
        }
    };

    private final CoverageElement parent;

    private CoverageElement() {
        this(null);
    }

    private CoverageElement(CoverageElement parent) {
        this.parent = parent;
    }

    /**
     * Getter for property 'parent'.
     *
     * @return Value for property 'parent'.
     */
    public CoverageElement getParent() {
        return parent;
    }

    /**
     * Return displayName of this coverage element.
     * <p/>
     * Note: This getter has to be evaluated each time in a non static
     * way because the user could change its language
     *
     * @return Value for property 'displayName'.
     */
    public abstract String getDisplayName();
}
