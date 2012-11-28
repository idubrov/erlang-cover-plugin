package hudson.plugins.erlangcover.otp;

import com.ericsson.otp.erlang.OtpErlangDecodeException;
import com.ericsson.otp.erlang.OtpErlangObject;
import com.ericsson.otp.erlang.OtpExternal;
import com.ericsson.otp.erlang.OtpInputStream;

import java.io.IOException;
import java.util.zip.Inflater;

/**
 * {@link OtpInputStream} with read_compressed fixed for Mac OS X.
 * @see <a href="http://erlang.org/pipermail/erlang-patches/2009-September/000478.html">Bug+patch: jinterface, OtpInputStream.java</a>
 */
public class FixedOtpInputStream extends OtpInputStream {
    private final int flags;

    public FixedOtpInputStream(byte[] buf) {
        super(buf);
        flags = 0;
    }

    public FixedOtpInputStream(byte[] buf, int flags) {
        super(buf, flags);
        this.flags = flags;
    }

    public FixedOtpInputStream(byte[] buf, int offset, int length, int flags) {
        super(buf, offset, length, flags);
        this.flags = flags;
    }

    public OtpErlangObject read_compressed() throws OtpErlangDecodeException {
        final int tag = read1skip_version();

        if (tag != OtpExternal.compressedTag) {
            throw new OtpErlangDecodeException(
                    "Wrong tag encountered, expected "
                            + OtpExternal.compressedTag + ", got " + tag);
        }

        final int size = read4BE();
        final byte[] buf = new byte[size];
        final java.util.zip.InflaterInputStream is =
                new java.util.zip.InflaterInputStream(this, new Inflater(), size);
        try {
            final int dsize = is.read(buf, 0, size);
            if (dsize != size) {
                throw new OtpErlangDecodeException("Decompression gave "
                        + dsize + " bytes, not " + size);
            }
        } catch (final IOException e) {
            throw new OtpErlangDecodeException("Cannot read from input stream");
        }

        final OtpInputStream ois = new OtpInputStream(buf, flags);
        return ois.read_any();
    }
}
