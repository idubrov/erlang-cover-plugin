package hudson.plugins.erlangcover;

import com.ericsson.otp.erlang.OtpErlangDecodeException;
import com.ericsson.otp.erlang.OtpInputStream;
import com.google.common.collect.Lists;
import com.google.common.io.Closeables;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.maven.ExecutedMojo;
import hudson.maven.MavenBuild;
import hudson.model.*;
import hudson.plugins.erlangcover.otp.FixedOtpInputStream;
import hudson.plugins.erlangcover.renderers.SourceCodePainter;
import hudson.plugins.erlangcover.renderers.SourceEncoding;
import hudson.plugins.erlangcover.targets.CoverageMetric;
import hudson.plugins.erlangcover.targets.CoverageResult;
import hudson.plugins.erlangcover.targets.CoverageTarget;
import hudson.remoting.VirtualChannel;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Cobertura {@link Publisher}.
 *
 * @author Stephen Connolly
 */
@SuppressWarnings("unused")
public class CoverPublisher extends Recorder {

    private final String coverFilePattern;
    private final boolean onlyStable;
    private final boolean failUnhealthy;
    private final boolean failUnstable;
    private final boolean autoUpdateHealth;
    private final boolean autoUpdateStability;

    private CoverageTarget healthyTarget;
    private CoverageTarget unhealthyTarget;
    private CoverageTarget failingTarget;
    private final SourceEncoding sourceEncoding;

    /**
     * @param coverFilePattern the cover data file pattern
     */
    @DataBoundConstructor
    public CoverPublisher(String coverFilePattern, boolean onlyStable, boolean failUnhealthy, boolean failUnstable, boolean autoUpdateHealth, boolean autoUpdateStability, SourceEncoding sourceEncoding) {
        this.coverFilePattern = coverFilePattern;
        this.onlyStable = onlyStable;
        this.failUnhealthy = failUnhealthy;
        this.failUnstable = failUnstable;
        this.autoUpdateHealth = autoUpdateHealth;
        this.autoUpdateStability = autoUpdateStability;
        this.sourceEncoding = sourceEncoding;
        this.healthyTarget = new CoverageTarget();
        this.unhealthyTarget = new CoverageTarget();
        this.failingTarget = new CoverageTarget();
    }

    /**
     * Getter for property 'targets'.
     *
     * @return Value for property 'targets'.
     */
    public List<CoverPublisherTarget> getTargets() {
        Map<CoverageMetric, CoverPublisherTarget> targets = new TreeMap<CoverageMetric, CoverPublisherTarget>();
        for (CoverageMetric metric : healthyTarget.getTargets()) {
            CoverPublisherTarget target = targets.get(metric);
            if (target == null) {
                target = new CoverPublisherTarget();
                target.setMetric(metric);
            }
            target.setHealthy(healthyTarget.getTarget(metric));
            targets.put(metric, target);
        }
        for (CoverageMetric metric : unhealthyTarget.getTargets()) {
            CoverPublisherTarget target = targets.get(metric);
            if (target == null) {
                target = new CoverPublisherTarget();
                target.setMetric(metric);
            }
            target.setUnhealthy(unhealthyTarget.getTarget(metric));
            targets.put(metric, target);
        }
        for (CoverageMetric metric : failingTarget.getTargets()) {
            CoverPublisherTarget target = targets.get(metric);
            if (target == null) {
                target = new CoverPublisherTarget();
                target.setMetric(metric);
            }
            target.setUnstable(failingTarget.getTarget(metric));
            targets.put(metric, target);
        }
        return new ArrayList<CoverPublisherTarget>(targets.values());
    }

    /**
     * Setter for property 'targets'.
     *
     * @param targets Value to set for property 'targets'.
     */
    private void setTargets(List<CoverPublisherTarget> targets) {
        healthyTarget.clear();
        unhealthyTarget.clear();
        failingTarget.clear();
        for (CoverPublisherTarget target : targets) {
            if (target.getHealthy() != null) {
                healthyTarget.setTarget(target.getMetric(), target.getHealthy());
            }
            if (target.getUnhealthy() != null) {
                unhealthyTarget.setTarget(target.getMetric(), target.getUnhealthy());
            }
            if (target.getUnstable() != null) {
                failingTarget.setTarget(target.getMetric(), target.getUnstable());
            }
        }
    }

    /**
     * Getter for property 'coverFilePattern'.
     *
     * @return Value for property 'coverFilePattern'.
     */
    public String getCoverFilePattern() {
        return coverFilePattern;
    }

    /**
     * Which type of build should be considered.
     *
     * @return the onlyStable
     */
    public boolean getOnlyStable() {
        return onlyStable;
    }

    /**
     * Getter for property 'failUnhealthy'.
     *
     * @return Value for property 'failUnhealthy'.
     */
    public boolean getFailUnhealthy() {
        return failUnhealthy;
    }

    /**
     * Getter for property 'failUnstable'.
     *
     * @return Value for property 'failUnstable'.
     */
    public boolean getFailUnstable() {
        return failUnstable;
    }

    /**
     * Getter for property 'autoUpdateHealth'.
     *
     * @return Value for property 'autoUpdateHealth'.
     */
    public boolean getAutoUpdateHealth() {
        return autoUpdateHealth;
    }

    /**
     * Getter for property 'autoUpdateStability'.
     *
     * @return Value for property 'autoUpdateStability'.
     */
    public boolean getAutoUpdateStability() {
        return autoUpdateStability;
    }

    /**
     * Getter for property 'healthyTarget'.
     *
     * @return Value for property 'healthyTarget'.
     */
    public CoverageTarget getHealthyTarget() {
        return healthyTarget;
    }

    /**
     * Setter for property 'healthyTarget'.
     *
     * @param healthyTarget Value to set for property 'healthyTarget'.
     */
    public void setHealthyTarget(CoverageTarget healthyTarget) {
        this.healthyTarget = healthyTarget;
    }

    /**
     * Getter for property 'unhealthyTarget'.
     *
     * @return Value for property 'unhealthyTarget'.
     */
    public CoverageTarget getUnhealthyTarget() {
        return unhealthyTarget;
    }

    /**
     * Setter for property 'unhealthyTarget'.
     *
     * @param unhealthyTarget Value to set for property 'unhealthyTarget'.
     */
    public void setUnhealthyTarget(CoverageTarget unhealthyTarget) {
        this.unhealthyTarget = unhealthyTarget;
    }

    /**
     * Getter for property 'failingTarget'.
     *
     * @return Value for property 'failingTarget'.
     */
    public CoverageTarget getFailingTarget() {
        return failingTarget;
    }

    /**
     * Setter for property 'failingTarget'.
     *
     * @param failingTarget Value to set for property 'failingTarget'.
     */
    public void setFailingTarget(CoverageTarget failingTarget) {
        this.failingTarget = failingTarget;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        Result threshold = onlyStable ? Result.SUCCESS : Result.UNSTABLE;
        if (build.getResult().isWorseThan(threshold)) {
            listener.getLogger().println("Skipping Cover coverage report as build was not " + threshold.toString() + " or better ...");
            return true;
        }

        listener.getLogger().println("Publishing Cover coverage report...");
        final FilePath[] moduleRoots = build.getModuleRoots();
        final boolean multipleModuleRoots =
                moduleRoots != null && moduleRoots.length > 1;
        final FilePath moduleRoot = multipleModuleRoots ? build.getWorkspace() : build.getModuleRoot();
        final File buildCoberturaDir = build.getRootDir();
        FilePath buildTarget = new FilePath(buildCoberturaDir);

        FilePath[] reports = new FilePath[0];
        try {
            reports = moduleRoot.act(new CollectCoverFilesCallable(coverFilePattern));

            // if the build has failed, then there's not
            // much point in reporting an error
            if (build.getResult().isWorseOrEqualTo(Result.FAILURE) && reports.length == 0)
                return true;

        } catch (IOException e) {
            Util.displayIOException(e, listener);
            e.printStackTrace(listener.fatalError("Unable to find coverage results"));
            build.setResult(Result.FAILURE);
        }

        if (reports.length == 0) {
            String msg = "No coverage results were found using the pattern '"
                    + coverFilePattern + "' relative to '"
                    + moduleRoot.getRemote() + "'."
                    + "  Did you enter a pattern relative to the correct directory?"
                    + "  Did you export the data file(s) for Cover?";
            listener.getLogger().println(msg);
            build.setResult(Result.FAILURE);
            return true;
        }

        for (int i = 0; i < reports.length; i++) {
            final FilePath targetPath = new FilePath(buildTarget, "cover" + (i == 0 ? "" : i) + ".data");
            try {
                reports[i].copyTo(targetPath);
            } catch (IOException e) {
                Util.displayIOException(e, listener);
                e.printStackTrace(listener.fatalError("Unable to copy cover data from " + reports[i] + " to " + buildTarget));
                build.setResult(Result.FAILURE);
            }
        }

        listener.getLogger().println("Publishing coverage results...");
        Set<String> sourcePaths = new HashSet<String>();
        CoverageResult result = null;
        for (File coverdata : getCoverData(build)) {
            try {
                result = CoverCoverageParser.parse(coverdata, result, sourcePaths);
            } catch (IOException e) {
                Util.displayIOException(e, listener);
                e.printStackTrace(listener.fatalError("Unable to parse " + coverdata));
                build.setResult(Result.FAILURE);
            }
        }
        if (result != null) {
            listener.getLogger().println("Cover coverage report found.");
            result.setOwner(build);
            final FilePath paintedSourcesPath = new FilePath(SourceCodePainter.paintedSourcesDirectory(build));
            paintedSourcesPath.mkdirs();
            SourceCodePainter painter = new SourceCodePainter(paintedSourcesPath, sourcePaths,
                    result.getPaintedSources(), listener, getSourceEncoding());

            moduleRoot.act(painter);

            final CoverBuildAction action = CoverBuildAction.load(build, result, healthyTarget,
                    unhealthyTarget, getOnlyStable(), getFailUnhealthy(), getFailUnstable(), getAutoUpdateHealth(), getAutoUpdateStability());

            build.getActions().add(action);
            Set<CoverageMetric> failingMetrics = failingTarget.getFailingMetrics(result);
            if (!failingMetrics.isEmpty()) {
                listener.getLogger().println("Code coverage enforcement failed for the following metrics:");
                float oldStabilityPercent;
                float setStabilityPercent;
                for (CoverageMetric metric : failingMetrics) {
                    oldStabilityPercent = failingTarget.getObservedPercent(result, metric);
                    setStabilityPercent = failingTarget.getSetPercent(result, metric);
                    listener.getLogger().println("    " + metric.getDisplayName() + "'s stability is " +
                            roundDecimalFloat(oldStabilityPercent * 100f) + " and set mininum stability is " + roundDecimalFloat(setStabilityPercent * 100f) + ".");
                }
                if (!getFailUnstable()) {
                    listener.getLogger().println("Setting Build to unstable.");
                    build.setResult(Result.UNSTABLE);
                } else {
                    listener.getLogger().println("Failing build due to unstability.");
                    build.setResult(Result.FAILURE);
                }
            }
            if (getFailUnhealthy()) {
                Set<CoverageMetric> unhealthyMetrics = unhealthyTarget.getFailingMetrics(result);
                if (!unhealthyMetrics.isEmpty()) {
                    listener.getLogger().println("Unhealthy for the following metrics:");
                    float oldHealthyPercent;
                    float setHealthyPercent;
                    for (CoverageMetric metric : unhealthyMetrics) {
                        oldHealthyPercent = unhealthyTarget.getObservedPercent(result, metric);
                        setHealthyPercent = unhealthyTarget.getSetPercent(result, metric);
                        listener.getLogger().println("    " + metric.getDisplayName() + "'s health is " +
                                roundDecimalFloat(oldHealthyPercent * 100f) + " and set minimum health is " + roundDecimalFloat(setHealthyPercent * 100f) + ".");
                    }
                    listener.getLogger().println("Failing build because it is unhealthy.");
                    build.setResult(Result.FAILURE);
                }
            }
            if (build.getResult() == Result.SUCCESS) {
                if (getAutoUpdateHealth()) {
                    setNewPercentages(result, true, listener);
                }

                if (getAutoUpdateStability()) {
                    setNewPercentages(result, false, listener);
                }
            }
        } else {
            listener.getLogger().println("No coverage results were successfully parsed.  Did you generate " +
                    "the XML report(s) for Cobertura?");
            build.setResult(Result.FAILURE);
        }

        return true;
    }

    /**
     * Changes unhealthy or unstable percentage fields for ratcheting.
     */
    private void setNewPercentages(CoverageResult result, boolean select, BuildListener listener) {
        Set<CoverageMetric> healthyMetrics = healthyTarget.getAllMetrics(result);
        float newPercent;
        float oldPercent;
        if (!healthyMetrics.isEmpty()) {
            for (CoverageMetric metric : healthyMetrics) {
                newPercent = healthyTarget.getObservedPercent(result, metric);
                if (select) {
                    oldPercent = unhealthyTarget.getSetPercent(result, metric);
                } else {
                    oldPercent = failingTarget.getSetPercent(result, metric);
                }
                if (newPercent > oldPercent) {
                    if (select) {
                        unhealthyTarget.setTarget(metric, newPercent);
                        listener.getLogger().println("    " + metric.getDisplayName() +
                                "'s new health minimum is: " + roundDecimalFloat(newPercent));
                    } else {
                        failingTarget.setTarget(metric, newPercent);
                        listener.getLogger().println("    " + metric.getDisplayName() +
                                "'s new stability minimum is: " + roundDecimalFloat(newPercent));
                    }
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Action getProjectAction(AbstractProject<?, ?> project) {
        return new CoverProjectAction(project, getOnlyStable());
    }

    /**
     * {@inheritDoc}
     */
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BuildStepDescriptor<Publisher> getDescriptor() {
        // see Descriptor javadoc for more about what a descriptor is.
        return DESCRIPTOR;
    }

    public SourceEncoding getSourceEncoding() {
        return sourceEncoding;
    }

    /**
     * Descriptor should be singleton.
     */
    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static class CollectCoverFilesCallable implements FilePath.FileCallable<FilePath[]> {
        private static final long serialVersionUID = 1L;

        private final String coverFilePattern;

        public CollectCoverFilesCallable(String coverFilePattern) {
            this.coverFilePattern = coverFilePattern;
        }

        public FilePath[] invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
            FilePath[] r = new FilePath(f).list(coverFilePattern);
            for (FilePath filePath : r) {
                InputStream in = filePath.read();
                try {
                    int size = in.read();
                    if (size == -1) {
                        throw new IOException(filePath + " is not a cover data file, please check your report pattern");
                    }
                    byte[] buf = new byte[size];
                    if (in.read(buf) != buf.length) {
                        throw new IOException(filePath + " is not a cover data file, please check your report pattern");
                    }
                    OtpInputStream ein = new FixedOtpInputStream(buf);
                    // Read one term to verify file looks good
                    ein.read_any();
                } catch (OtpErlangDecodeException e) {
                    throw new IOException(filePath + " is not a cover data file, please check your report pattern");
                } finally {
                    Closeables.closeQuietly(in);
                }
            }
            return r;
        }
    }

    /**
     * Descriptor for {@link CoverPublisher}. Used as a singleton. The class is marked as public so that it can be
     * accessed from views.
     * <p/>
     * <p/>
     * See <tt>views/hudson/plugins/erlangcover/CoverPublisher/*.jelly</tt> for the actual HTML fragment for the
     * configuration screen.
     */
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        /**
         * Constructs a new DescriptorImpl.
         */
        DescriptorImpl() {
            super(CoverPublisher.class);
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return Messages.CoverPublisher_displayName();
        }

        /**
         * Getter for property 'metrics'.
         *
         * @return Value for property 'metrics'.
         */
        public List<CoverageMetric> getMetrics() {
            return Arrays.asList(CoverageMetric.values());
        }

        /**
         * Getter for property 'defaultTargets'.
         *
         * @return Value for property 'defaultTargets'.
         */
        public List<CoverPublisherTarget> getDefaultTargets() {
            return Lists.newArrayList(
                    new CoverPublisherTarget(CoverageMetric.FUNCTION, 80f, null, null),
                    new CoverPublisherTarget(CoverageMetric.LINE, 80f, null, null));
        }

        public List<CoverPublisherTarget> getTargets(CoverPublisher instance) {
            if (instance == null) {
                return getDefaultTargets();
            }
            return instance.getTargets();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            req.bindParameters(this, "cover.");
            save();
            return super.configure(req, formData);
        }

        /**
         * Creates a new instance of {@link CoverPublisher} from a submitted form.
         */
        @Override
        public CoverPublisher newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            CoverPublisher instance = req.bindJSON(CoverPublisher.class, formData);
            //ConvertUtils.register(CoverPublisherTarget.CONVERTER, CoverageMetric.class);
            List<CoverPublisherTarget> targets = req
                    .bindParametersToList(CoverPublisherTarget.class, "cover.target.");
            instance.setTargets(targets);
            return instance;
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }
    }


    /**
     * Gets list of cover data files.
     */
    public static File[] getCoverData(AbstractBuild<?, ?> build) {
        return build.getRootDir().listFiles(CoverDataFilter.INSTANCE);
    }

    private enum CoverDataFilter implements FilenameFilter {
        INSTANCE;

        /**
         * {@inheritDoc}
         */
        public boolean accept(File dir, String name) {
            return name.startsWith("cover") && name.endsWith(".data");
        }
    }

    private boolean didCoberturaRun(Iterable<MavenBuild> mavenBuilds) {
        for (MavenBuild build : mavenBuilds) {
            if (didCoberturaRun(build)) return true;
        }
        return false;
    }

    private boolean didCoberturaRun(MavenBuild mavenBuild) {
        for (ExecutedMojo mojo : mavenBuild.getExecutedMojos()) {
            if (mojo.groupId.equals("org.codehaus.mojo") &&
                    (mojo.artifactId.equals("erlangcover-maven-plugin") || mojo.artifactId.equals("erlangcover-it-maven-plugin"))) {
                return true;
            }
        }
        return false;
    }

    public float roundDecimalFloat(Float input) {
        float rounded = (float) Math.round(input);
        rounded = rounded / 100f;
        return rounded;
    }
}
