package hudson.plugins.erlangcover;

import hudson.model.*;
import hudson.util.Graph;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.io.IOException;

/**
 * Project level action.
 *
 * @author Stephen Connolly
 */
public class CoverProjectAction extends Actionable implements ProminentProjectAction {

    private final AbstractProject<?, ?> project;
    private boolean onlyStable;

    public CoverProjectAction(AbstractProject<?, ?> project, boolean onlyStable) {
        this.project = project;
        this.onlyStable = onlyStable;
    }

    public CoverProjectAction(AbstractProject<?, ?> project) {
        this.project = project;
        
        CoverPublisher cp = (CoverPublisher) project.getPublishersList().get(CoverPublisher.DESCRIPTOR);
        if (cp != null) {
            onlyStable = cp.getOnlyStable();
        }
    }
    
    public AbstractProject<?, ?> getProject() {
        return project;
    }

    /**
     * {@inheritDoc}
     */
    public String getIconFileName() {
        return "graph.gif";
    }

    /**
     * {@inheritDoc}
     */
    public String getDisplayName() {
        return Messages.CoverProjectAction_displayName();
    }

    /**
     * {@inheritDoc}
     */
    public String getUrlName() {
        return "erlangcover";
    }

    /**
     * Getter for property 'lastResult'.
     *
     * @return Value for property 'lastResult'.
     */
    public CoverBuildAction getLastResult() {

        for (AbstractBuild<?, ?> b = getLastBuildToBeConsidered(); b != null; b = b.getPreviousNotFailedBuild()) {
            if (b.getResult() == Result.FAILURE || (b.getResult() != Result.SUCCESS && onlyStable))
                continue;
            CoverBuildAction r = b.getAction(CoverBuildAction.class);
            if (r != null) {
                return r;
            }
        }
        return null;
    }

    private AbstractBuild<?, ?> getLastBuildToBeConsidered() {
        return onlyStable ? project.getLastStableBuild() : project.getLastSuccessfulBuild();
    }
     /**
     * Getter for property 'lastResult'.
     *
     * @return Value for property 'lastResult'.
     */
    public Integer getLastResultBuild() {
        for (AbstractBuild<?, ?> b = getLastBuildToBeConsidered(); b != null; b = b.getPreviousNotFailedBuild()) {
            if (b.getResult() == Result.FAILURE || (b.getResult() != Result.SUCCESS && onlyStable))
                continue;
            CoverBuildAction r = b.getAction(CoverBuildAction.class);
            if (r != null)
                return b.getNumber();
        }
        return null;
    }

    public Graph getGraph() throws IOException {
        return (getLastResult() != null) ? getLastResult().getGraph() : null;
    }

    public void doIndex(StaplerRequest req, StaplerResponse rsp) throws IOException {
        Integer buildNumber = getLastResultBuild();
        if (buildNumber == null) {
            rsp.sendRedirect2("nodata");
        } else {
            rsp.sendRedirect2("../" + buildNumber + "/erlangcover");
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getSearchUrl() {
        return getUrlName();
    }
}
