package hudson.plugins.erlangcover;

import hudson.model.AbstractBuild;
import hudson.plugins.erlangcover.targets.CoverageElement;
import hudson.plugins.erlangcover.targets.CoverageMetric;
import hudson.plugins.erlangcover.targets.CoverageResult;
import junit.framework.TestCase;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import java.io.InputStream;
import java.util.*;

/**
 * Unit tests for {@link CoverageResult}.
 */
public class CoverageResultTest extends TestCase {
    private static final String FILE_COVERAGE_DATA = "coverage-with-data.coverdata";
    private IMocksControl ctl;
    private AbstractBuild<?, ?> build;

    /**
     * Set up the mock objects used by the tests.
     */
     protected void setUp() throws Exception {
          super.setUp();
        ctl = EasyMock.createControl();
        build = ctl.createMock("build", AbstractBuild.class);
    }

    /**
     * Parses a coverage XML file into a CoverageResult object.
     *
     * @param fileName the name of the resource to parse
     * @return a CoverageResult object
     */
    private CoverageResult loadResults(String fileName) throws Exception {
        InputStream in = getClass().getResourceAsStream(fileName);
        CoverageResult result = CoverCoverageParser.parse(in, null);
        return result;
    }

    /**
     * Tests the behavior of {@link CoverageResult#setOwner(AbstractBuild)}.
     */
    public void testSetOwner() throws Exception {
        ctl.replay();
        CoverageResult result = loadResults(FILE_COVERAGE_DATA);
        assertNull(result.getOwner());
        result.setOwner(build);
        assertSame(build, result.getOwner());
        ctl.verify();
    }

    /**
     * Tests the behavior of {@link CoverageResult#getResults()}.
     */
    public void testGetResults() throws Exception {
        ctl.replay();
        CoverageResult result = loadResults(FILE_COVERAGE_DATA);
        assertEquals(Collections.EMPTY_MAP, result.getResults());
        result.setOwner(build);
        Map<CoverageMetric,Ratio> metrics = result.getResults();
        assertEquals(3, result.getResults().size());
        assertEquals(Ratio.create(42, 42), metrics.get(CoverageMetric.MODULES));
        assertEquals(Ratio.create(320, 431), metrics.get(CoverageMetric.FUNCTION));
        assertEquals(Ratio.create(500, 731), metrics.get(CoverageMetric.LINE));
        ctl.verify();
    }

    /**
     * Tests the behavior of {@link CoverageResult#getParent()}.
     */
    public void testGetParent() throws Exception {
        ctl.replay();
        // Project level
        CoverageResult result = loadResults(FILE_COVERAGE_DATA);
        result.setOwner(build);
        assertNull(result.getParent());
        // Module level
        CoverageResult expectedParent = result;
        System.err.println(result.getChildElements().iterator().next().getDisplayName());
        result = result.getChild("sip_app");
        assertSame(expectedParent, result.getParent());
        // Function level
        expectedParent = result;
        result = result.getChild("stop/1");
        assertSame(expectedParent, result.getParent());
        ctl.verify();
    }

    /**
     * Tests the behavior of {@link CoverageResult#getParents()}.
     */
    public void testGetParents() throws Exception {
        ctl.replay();
        // Project level
        LinkedList<CoverageResult> expectedParents = new LinkedList<CoverageResult>();
        CoverageResult result = loadResults(FILE_COVERAGE_DATA);
        result.setOwner(build);
        assertEquals(expectedParents, result.getParents());
        // Package level
        expectedParents.add(result);
        result = result.getChild("sip_app");
        assertEquals(expectedParents, result.getParents());
        // File level
        expectedParents.add(result);
        result = result.getChild("stop/1");
        assertEquals(expectedParents, result.getParents());
        ctl.verify();
    }

    /**
     * Tests the behavior of {@link CoverageResult#getChildElements()}.
     */
    public void testGetChildElements() throws Exception {
        ctl.replay();
        // Project level
        CoverageResult result = loadResults(FILE_COVERAGE_DATA);
        result.setOwner(build);
        assertEquals(Collections.singleton(CoverageElement.ERLANG_MODULE), result.getChildElements());
        // Module level
        result = result.getChild("sip_app");
        assertEquals(Collections.singleton(CoverageElement.ERLANG_FUNCTION), result.getChildElements());
        // Function level
        result = result.getChild("stop/1");
        assertEquals(Collections.emptySet(), result.getChildElements());
        ctl.verify();
    }

    /**
     * Tests the behavior of {@link CoverageResult#getChildren()}.
     */
    public void testGetChildren() throws Exception {
        ctl.replay();
        // Project level
        CoverageResult result = loadResults(FILE_COVERAGE_DATA);
        result.setOwner(build);
        assertEquals(new HashSet<String>(Arrays.asList(new String[] {"sip_app", "sip_binary", "sip_config",
                "sip_cores", "sip_dialog", "sip_dialog_ets", "sip_headers", "sip_idgen", "sip_log", "sip_message",
                "sip_priority_set", "sip_resolve", "sip_session", "sip_sup", "sip_syntax", "sip_transaction",
                "sip_transaction_base", "sip_transaction_client", "sip_transaction_client_invite",
                "sip_transaction_server", "sip_transaction_server_invite", "sip_transaction_sup",
                "sip_transaction_tx_sup", "sip_transport", "sip_transport_icmp", "sip_transport_sup",
                "sip_transport_tcp", "sip_transport_tcp_conn", "sip_transport_tcp_conn_sup",
                "sip_transport_tcp_listener", "sip_transport_tcp_sup", "sip_transport_udp",
                "sip_transport_udp_socket", "sip_transport_udp_socket_sup", "sip_transport_udp_sup", "sip_ua",
                "sip_ua_client", "sip_ua_default", "sip_ua_server", "sip_ua_session", "sip_ua_sup", "sip_uri"})), result.getChildren());
        // Class level
        result = result.getChild("sip_app");
        assertEquals(new HashSet<String>(Arrays.asList(new String[] {"start/2", "stop/1"})), result.getChildren());
        // Function level
        result = result.getChild("stop/1");
        assertEquals(Collections.emptySet(), result.getChildren());
        ctl.verify();
    }

    /**
     * Tests the behavior of {@link CoverageResult#getChildren(CoverageElement)}.
     */
    public void testGetChildrenCoverageElement() throws Exception {
        ctl.replay();
        // Project level
        CoverageResult result = loadResults(FILE_COVERAGE_DATA);
        result.setOwner(build);
        assertEquals(new HashSet<String>(Arrays.asList(new String[] {"sip_app", "sip_binary", "sip_config",
                "sip_cores", "sip_dialog", "sip_dialog_ets", "sip_headers", "sip_idgen", "sip_log", "sip_message",
                "sip_priority_set", "sip_resolve", "sip_session", "sip_sup", "sip_syntax", "sip_transaction",
                "sip_transaction_base", "sip_transaction_client", "sip_transaction_client_invite",
                "sip_transaction_server", "sip_transaction_server_invite", "sip_transaction_sup",
                "sip_transaction_tx_sup", "sip_transport", "sip_transport_icmp", "sip_transport_sup",
                "sip_transport_tcp", "sip_transport_tcp_conn", "sip_transport_tcp_conn_sup",
                "sip_transport_tcp_listener", "sip_transport_tcp_sup", "sip_transport_udp",
                "sip_transport_udp_socket", "sip_transport_udp_socket_sup", "sip_transport_udp_sup", "sip_ua",
                "sip_ua_client", "sip_ua_default", "sip_ua_server", "sip_ua_session", "sip_ua_sup", "sip_uri"})),
                result.getChildren(CoverageElement.ERLANG_MODULE));
        assertEquals(Collections.emptySet(), result.getChildren(CoverageElement.PROJECT));
        assertEquals(Collections.emptySet(), result.getChildren(CoverageElement.ERLANG_FUNCTION));
        // Module level
        result = result.getChild("sip_app");
        assertEquals(new HashSet<String>(Arrays.asList(new String[] {"start/2", "stop/1"})),
                result.getChildren(CoverageElement.ERLANG_FUNCTION));
        assertEquals(Collections.emptySet(), result.getChildren(CoverageElement.PROJECT));
        assertEquals(Collections.emptySet(), result.getChildren(CoverageElement.ERLANG_MODULE));
        // Function level
        result = result.getChild("stop/1");
        assertEquals(Collections.emptySet(), result.getChildren(CoverageElement.ERLANG_MODULE));
        assertEquals(Collections.emptySet(), result.getChildren(CoverageElement.PROJECT));
        assertEquals(Collections.emptySet(), result.getChildren(CoverageElement.ERLANG_FUNCTION));
        ctl.verify();
    }

    /**
     * Tests the behavior of {@link CoverageResult#getChildMetrics(CoverageElement)}.
     */
    public void testGetChildMetricsCoverageElement() throws Exception {
        ctl.replay();
        // Project level
        CoverageResult result = loadResults(FILE_COVERAGE_DATA);
        result.setOwner(build);
        assertEquals(new HashSet<CoverageMetric>(Arrays.asList(new CoverageMetric[] {CoverageMetric.FUNCTION,
                CoverageMetric.LINE})), result.getChildMetrics(CoverageElement.ERLANG_MODULE));
        assertEquals(Collections.EMPTY_SET, result.getChildMetrics(CoverageElement.PROJECT));
        assertEquals(Collections.EMPTY_SET, result.getChildMetrics(CoverageElement.ERLANG_FUNCTION));
        // Module level
        result = result.getChild("sip_app");
        assertEquals(new HashSet<CoverageMetric>(Arrays.asList(new CoverageMetric[] {CoverageMetric.LINE})),
                result.getChildMetrics(CoverageElement.ERLANG_FUNCTION));
        assertEquals(Collections.EMPTY_SET, result.getChildMetrics(CoverageElement.PROJECT));
        assertEquals(Collections.EMPTY_SET, result.getChildMetrics(CoverageElement.ERLANG_MODULE));
        // Function level
        result = result.getChild("stop/1");
        assertEquals(Collections.EMPTY_SET, result.getChildMetrics(CoverageElement.PROJECT));
        assertEquals(Collections.EMPTY_SET, result.getChildMetrics(CoverageElement.ERLANG_MODULE));
        assertEquals(Collections.EMPTY_SET, result.getChildMetrics(CoverageElement.ERLANG_FUNCTION));
        ctl.verify();
    }
}
