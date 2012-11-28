package hudson.plugins.erlangcover;

import com.ericsson.otp.erlang.*;
import com.google.common.base.Preconditions;
import com.google.common.io.Closeables;
import hudson.FilePath;
import hudson.plugins.erlangcover.otp.FixedOtpInputStream;
import hudson.plugins.erlangcover.targets.CoverageElement;
import hudson.plugins.erlangcover.targets.CoverageMetric;
import hudson.plugins.erlangcover.targets.CoverageResult;

import java.io.*;
import java.util.Set;

/**
 * Parse Erlang Cover data file.
 * <p/>
 * Binary file format is repetition of the following sections:
 * <code>
 * size (one byte).
 * term : &lt;size&gt; bytes.
 * If &lt;term&gt; is <code>{'$size', size2}</code>, then read next term of size &lt;size2&gt;
 * </code>
 * <p/>
 * Structure after deserializing:
 * <code>
 * [{file, Module, BeamFile} |
 * {Module, [{Module, Function, Arity, Clause, LinesCnt}]} |
 * {{bump, Module, Function, Arity, Clause, Line}, HitCnt}]
 * </code>
 *
 * @author idubrov
 */
public class CoverCoverageParser {

    private static OtpErlangAtom SIZE_ATOM = new OtpErlangAtom("$size");
    private static OtpErlangAtom BUMP_ATOM = new OtpErlangAtom("bump");
    private static OtpErlangAtom FILE_ATOM = new OtpErlangAtom("file");

    /**
     * Do not instantiate CoverCoverageParser.
     */
    private CoverCoverageParser() {
    }

    public static CoverageResult parse(File inFile, CoverageResult cumulative) throws IOException {
        return parse(inFile, cumulative, null);
    }

    public static CoverageResult parse(File inFile, CoverageResult cumulative, Set<String> sourcePaths) throws IOException {
        InputStream fin = new FileInputStream(inFile);
        try {
            InputStream in = new BufferedInputStream(fin);
            try {
                return parse(in, cumulative, sourcePaths);
            } finally {
                Closeables.closeQuietly(in);
            }
        } finally {
            Closeables.closeQuietly(fin);
        }
    }

    private static void bump(CoverageResult rootCoverage, String module, String function, int line, int hits) {
        CoverageResult result = rootCoverage
                .createChild(CoverageElement.ERLANG_MODULE, module)
                .createChild(CoverageElement.ERLANG_FUNCTION, function);

        // FIXME: Clause painting?...
        result.paint(line, hits);
        result.updateMetric(CoverageMetric.LINE, Ratio.create((hits == 0) ? 0 : 1, 1));
    }

    public static CoverageResult parse(InputStream in, CoverageResult rootCoverage) throws IOException {
        return parse(in, rootCoverage, null);
    }

    public static CoverageResult parse(InputStream in, CoverageResult rootCoverage, Set<String> sourcePaths) throws IOException {
        Preconditions.checkNotNull(in);

        if (rootCoverage == null) {
            rootCoverage = new CoverageResult(CoverageElement.PROJECT, null, Messages.CoverCoverageParser_name());
        }
        try {
            CoverageResult moduleResult = null;
            OtpErlangObject term;
            while ((term = readTerm(in)) != null) {
                if (term instanceof OtpErlangTuple) {
                    OtpErlangTuple tuple = (OtpErlangTuple) term;
                    if (tuple.arity() == 3 && FILE_ATOM.equals(tuple.elementAt(0))) {
                        // {file,sip_ua_client,"/Users/idubrov/Projects/siperl/apps/sip/ebin/sip_ua_client.beam"}
                        String module = eatom(tuple, 1);
                        String path = estring(tuple, 2);

                        if (sourcePaths != null) {
                            // Let's guess source paths
                            // FIXME: What if path was generated on different OS?
                            // Remove ebin/<file>.beam or .eunit/<file>.beam
                            String sourcePath = new File(path).getParentFile().getParentFile().getAbsolutePath();
                            sourcePaths.add(sourcePath + "/src");
                        }

                        moduleResult = rootCoverage
                                .createChild(CoverageElement.ERLANG_MODULE, module);
                        moduleResult.setRelativeSourcePath(module + ".erl");
                    } else if (tuple.arity() == 2 && tuple.elementAt(0) instanceof OtpErlangTuple) {
                        //{{bump, Module, Function, Arity, Clause, Line}, HitCnt}]
                        OtpErlangTuple tuple2 = (OtpErlangTuple) tuple.elementAt(0);
                        if (tuple2.arity() == 6 && BUMP_ATOM.equals(tuple2.elementAt(0))) {
                            String module = eatom(tuple2, 1);
                            String function = eatom(tuple2, 2);
                            int arity = eint(tuple2, 3);
                            int clause = eint(tuple2, 4);
                            int line = eint(tuple2, 5);
                            int hits = eint(tuple, 1);

                            // Ignore generated functions
                            if (line != 0) {
                                // FIXME: moduleResult should be non-null!

                                String funcName = function + '/' + arity;
                                CoverageResult funcResult =
                                        moduleResult.createChild(CoverageElement.ERLANG_FUNCTION, funcName);

                                // FIXME: Clause painting?...
                                funcResult.setRelativeSourcePath(moduleResult.getRelativeSourcePath());
                                funcResult.paint(line, hits);
                                funcResult.updateMetric(CoverageMetric.LINE, Ratio.create((hits == 0) ? 0 : 1, 1));
                            }
                        }
                    }
                }
                //System.err.println(term);
            }
        } catch (OtpErlangException e) {
            throw new IOException("File is not valid cover data file.", e);
        }
        return rootCoverage;
    }

    private static String eatom(OtpErlangTuple tuple, int pos) {
        return ((OtpErlangAtom) tuple.elementAt(pos)).atomValue();
    }

    private static int eint(OtpErlangTuple tuple, int pos) throws OtpErlangRangeException {
        return ((OtpErlangLong) tuple.elementAt(pos)).intValue();
    }

    private static String estring(OtpErlangTuple tuple, int pos) {
        return ((OtpErlangString) tuple.elementAt(pos)).stringValue();
    }

    /**
     * Java implementation of cover:get_term/1
     *
     * @param in
     * @return
     * @throws IOException
     * @throws OtpErlangDecodeException
     */
    private static OtpErlangObject readTerm(InputStream in) throws IOException, OtpErlangException {
        int size = in.read();
        if (size == -1) {
            return null; // EOF
        }
        OtpErlangObject term = readTerm(in, size);

        // match {'$size', size2}
        if (term instanceof OtpErlangTuple) {
            OtpErlangTuple tuple = (OtpErlangTuple) term;
            if (tuple.arity() == 2 && SIZE_ATOM.equals(tuple.elementAt(0))) {
                OtpErlangLong erlangInt = (OtpErlangLong) tuple.elementAt(1);
                term = readTerm(in, erlangInt.intValue());
            }
        }
        return term;
    }

    private static OtpErlangObject readTerm(InputStream in, int size) throws IOException, OtpErlangDecodeException {
        byte[] buf = new byte[size];
        if (in.read(buf) != buf.length) {
            throw new EOFException("File is not valid cover data file.");
        }
        OtpInputStream ein = new FixedOtpInputStream(buf);
        OtpErlangObject term = ein.read_any();
        return term;
    }
}

