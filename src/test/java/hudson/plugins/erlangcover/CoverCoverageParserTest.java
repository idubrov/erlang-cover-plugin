package hudson.plugins.erlangcover;

import hudson.plugins.erlangcover.targets.CoverageMetric;
import hudson.plugins.erlangcover.targets.CoverageResult;
import junit.framework.TestCase;
import org.netbeans.insane.scanner.CountingVisitor;
import org.netbeans.insane.scanner.ScannerUtils;

import java.io.InputStream;
import java.util.*;

/**
 * Unit tests for {@link CoverCoverageParser}.
 */
public class CoverCoverageParserTest extends TestCase {
    public CoverCoverageParserTest(String name) {
        super(name);
    }

    public void testFailureMode1() throws Exception {
        try {
            CoverCoverageParser.parse((InputStream) null, null);
        } catch (NullPointerException e) {
            assertTrue("Expected exception thrown", true);
        }
    }

    public void print(CoverageResult r, int d) {
        System.out.print("                    ".substring(0, d*2));
        System.out.print(r.getElement() + "[" + r.getName() + "]");
        for (CoverageMetric m : r.getMetrics()) {
            System.out.print(" " + m + "=" + r.getCoverage(m));
        }
        System.out.println();
        for (String child: r.getChildren()) {
            print(r.getChild(child), d + 1);
        }
    }

    public void testParse() throws Exception {
        Set<String> paths = new HashSet<String>();
        CoverageResult result = CoverCoverageParser.parse(getClass().getResourceAsStream("coverage.xml"), null, paths);
        result.setOwner(null);
        print(result, 0);
        assertNotNull(result);
        assertEquals(CoverageResult.class, result.getClass());
        assertEquals(Messages.CoverCoverageParser_name(), result.getName());
//        assertEquals(10, result.getMethods());
        assertEquals(2, result.getChildren().size());
        CoverageResult subResult = result.getChild("<default>");
        assertEquals("<default>", subResult.getName());
        assertEquals(1, subResult.getChildren().size());
        assertEquals(Ratio.create(0, 3), subResult.getCoverage(CoverageMetric.FUNCTION));
        assertEquals(Ratio.create(0, 11), subResult.getCoverage(CoverageMetric.LINE));
        subResult = result.getChild("search");
        assertEquals("search", subResult.getName());
        assertEquals(3, subResult.getChildren().size());
        assertEquals(Ratio.create(0, 19), subResult.getCoverage(CoverageMetric.LINE));
        assertEquals(Ratio.create(0, 4), subResult.getCoverage(CoverageMetric.FUNCTION));
        assertEquals(1, paths.size());
    }

    public void testParse2() throws Exception {
        CoverageResult result = CoverCoverageParser.parse(getClass().getResourceAsStream("coverage-with-data.coverdata"), null);
        result.setOwner(null);
        print(result, 0);
        assertNotNull(result);
        assertEquals(CoverageResult.class, result.getClass());
        assertEquals(Messages.CoverCoverageParser_name(), result.getName());
//        assertEquals(10, result.getMethods());
        assertEquals(2, result.getChildren().size());
        CoverageResult subResult = result.getChild("<default>");
        assertEquals("<default>", subResult.getName());
        assertEquals(1, subResult.getChildren().size());
        assertEquals(Ratio.create(3, 3), subResult.getCoverage(CoverageMetric.FUNCTION));
        assertEquals(Ratio.create(11, 11), subResult.getCoverage(CoverageMetric.LINE));
        subResult = result.getChild("search");
        assertEquals("search", subResult.getName());
        assertEquals(3, subResult.getChildren().size());
        assertEquals(Ratio.create(16, 19), subResult.getCoverage(CoverageMetric.LINE));
        assertEquals(Ratio.create(4, 4), subResult.getCoverage(CoverageMetric.FUNCTION));
    }

    /**
     * Tests the memory usage of
     * {@link CoverCoverageParser#parse(InputStream, CoverageResult, Set)}.
     *
     * @since 28-Apr-2009
     */
    public void testParseMemoryUsage() throws Exception {
        Map<String,Integer> files = new LinkedHashMap<String,Integer>();
        files.put("coverage.xml", 16152);
        files.put("coverage-with-data.xml", 16232);
        files.put("coverage-with-lots-of-data.xml", 298960);

        for (Map.Entry<String,Integer> e : files.entrySet()) {
            final String fileName = e.getKey();
            InputStream in = getClass().getResourceAsStream(fileName);
            CoverageResult result = CoverCoverageParser.parse(in, null, null);
            result.setOwner(null);
            assertMaxMemoryUsage(fileName + " results", result, e.getValue());
        }
    }

    /**
     * Tests the memory usage of a specified object.
     * The memory usage is then compared with the specified
     * maximum desired memory usage.  If the average memory usage is greater
     * than the specified number, it will be reported as a failed assertion.
     *
     * @param description a plain-text description, to be used
     *          in diagnostic messages
     * @param o the object to measure
     * @param maxMemoryUsage the maximum desired memory usage for the Callable,
     *          in bytes
     */
    private static void assertMaxMemoryUsage(String description, Object o, int maxMemoryUsage) throws Exception {
        CountingVisitor v = new CountingVisitor();
        ScannerUtils.scan(null, v, Collections.singleton(o), false);
        long memoryUsage = v.getTotalSize();
        String message = description + " consume " + memoryUsage + " bytes of memory on average, " + (memoryUsage - maxMemoryUsage) + " bytes more than the specified limit of " + maxMemoryUsage + " bytes";
        assertTrue(message, memoryUsage <= maxMemoryUsage);
        System.out.println(description + " consume " + memoryUsage + "/" + maxMemoryUsage + " bytes of memory");
    }

}
