package hudson.plugins.erlangcover.dashboard;

import com.google.common.collect.Maps;
import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.Job;
import hudson.model.Run;
import hudson.plugins.erlangcover.CoverBuildAction;
import hudson.plugins.erlangcover.Ratio;
import hudson.plugins.erlangcover.targets.CoverageMetric;
import hudson.plugins.erlangcover.targets.CoverageResult;
import hudson.plugins.view.dashboard.DashboardPortlet;

import java.util.*;

import org.kohsuke.stapler.DataBoundConstructor;

public class CoverageTablePortlet extends DashboardPortlet {

    @DataBoundConstructor
    public CoverageTablePortlet(String name) {
        super(name);
    }

    public Collection<Run> getCoverageRuns() {
        LinkedList<Run> allResults = new LinkedList<Run>();

        for (Job job : getDashboard().getJobs()) {
            // Find the latest successful coverage data
            Run run = job.getLastSuccessfulBuild();
            if (run == null) continue;

            CoverBuildAction rbb = run
                    .getAction(CoverBuildAction.class);

            if (rbb != null) {
                allResults.add(run);
            }
        }

        return allResults;
    }

    public CoverageResult getCoverageResult(Run run) {
        CoverBuildAction rbb = run.getAction(CoverBuildAction.class);
        return rbb.getResult();
    }

    public EnumMap<CoverageMetric, Ratio> getTotalCoverageRatio() {
        EnumMap<CoverageMetric, Ratio> totalRatioMap = Maps.newEnumMap(CoverageMetric.class);
        for (Job job : getDashboard().getJobs()) {
            // Find the latest successful coverage data
            Run run = job.getLastSuccessfulBuild();
            if (run == null) {
                continue;
            }

            CoverBuildAction rbb = run
                    .getAction(CoverBuildAction.class);
            if (rbb == null) {
                continue;
            }

            CoverageResult result = rbb.getResult();
            Set<CoverageMetric> metrics = result.getMetrics();
            for (CoverageMetric metric : metrics) {
                if (totalRatioMap.get(metric) == null) {
                    totalRatioMap.put(metric, result.getCoverage(metric));
                } else {
                    int currentNumerator = totalRatioMap.get(metric).numerator;
                    int currentDenominator = totalRatioMap.get(metric).denominator;
                    int sumNumerator = currentNumerator + result.getCoverage(metric).numerator;
                    int sumDenominator = currentDenominator + result.getCoverage(metric).denominator;
                    totalRatioMap.put(metric, Ratio.create(sumNumerator, sumDenominator));
                }
            }
        }
        return totalRatioMap;
    }

    public static class DescriptorImpl extends Descriptor<DashboardPortlet> {

        @Extension(optional = true)
        public static DescriptorImpl newInstance() {
            if (Hudson.getInstance().getPlugin("dashboard-view") != null) {
                return new DescriptorImpl();
            } else {
                return null;
            }
        }

        @Override
        public String getDisplayName() {
            return "Code Coverages (Erlang cover)";
        }
    }
}
