package hudson.plugins.erlangcover.targets;

import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import hudson.plugins.erlangcover.Ratio;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

/**
 * Line-by-line coverage information.
 *
 * @author Stephen Connolly
 * @since 29-Aug-2007 17:44:29
 */
public class CoveragePaint implements Serializable {

    /**
     * Generated
     */
    private static final long serialVersionUID = -6265259191856193735L;
    private static final CoveragePaintDetails[] EMPTY = new CoveragePaintDetails[0];
    private static class CoveragePaintDetails implements Serializable {
        /**
         * Generated
         */
        private static final long serialVersionUID = -8795145975629453028L;

        /**
         * Fly-weight object pool of (n,0,0) instances, which are very common.
         */
        private static final CoveragePaintDetails[] CONSTANTS = new CoveragePaintDetails[128];


        /**
         * Number of times this line is executed.
         */
        final int hitCount;

        static CoveragePaintDetails create(int hitCount) {
            if (0 <= hitCount && hitCount < CONSTANTS.length) {
                CoveragePaintDetails r = CONSTANTS[hitCount];
                if (r == null) CONSTANTS[hitCount] = r = new CoveragePaintDetails(hitCount);
                return r;
            }
            return new CoveragePaintDetails(hitCount);
        }

        private CoveragePaintDetails(int hitCount) {
            this.hitCount = hitCount;
        }

        /**
         * Do 'this+that' and return the new object.
         */
        CoveragePaintDetails add(CoveragePaintDetails that) {
            return CoveragePaintDetails.create(
                    this.hitCount + that.hitCount);
        }
    }

    private TIntObjectMap<CoveragePaintDetails> lines = new TIntObjectHashMap<CoveragePaintDetails>();
    private Integer firstLine;

    public Integer getFirstLine() {
        return firstLine;
    }

    private void paint(int line, CoveragePaintDetails delta) {
        CoveragePaintDetails d = lines.get(line);
        d = (d == null) ? delta : d.add(delta);
        lines.put(line, d);

        if (firstLine == null || line < firstLine) {
            firstLine = line;
        }
    }

    public void paint(int line, int hits) {
        paint(line, CoveragePaintDetails.create(hits));
    }

    public void add(CoveragePaint child) {
        TIntObjectIterator<CoveragePaintDetails> it = child.lines.iterator();
        while (it.hasNext()) {
            it.advance();
            paint(it.key(), it.value());
        }
    }

    /**
     * Getter for property 'lineCoverage'.
     *
     * @return Value for property 'lineCoverage'.
     */
    public Ratio getLineCoverage() {
        int covered = 0;
        for (CoveragePaintDetails d : lines.values(EMPTY)) {
            if (d.hitCount > 0) {
                covered++;
            }
        }
        return Ratio.create(covered, lines.size());
    }

    /**
     * Getter for property 'results'.
     *
     * @return Value for property 'results'.
     */
    public Map<CoverageMetric, Ratio> getResults() {
        return Collections.singletonMap(CoverageMetric.LINE, getLineCoverage());
    }

    public boolean isPainted(int line) {
        return lines.get(line) != null;
    }

    public int getHits(int line) {
        CoveragePaintDetails details = lines.get(line);
        return (details != null) ? details.hitCount : 0;
    }
}
