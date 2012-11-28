package hudson.plugins.erlangcover;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * Represents <tt>x/y</tt> where x={@link #numerator} and y={@link #denominator}.
 *
 * @author Kohsuke Kawaguchi
 */
final public class Ratio implements Serializable {
    private static final NumberFormat DATA_FORMAT = new DecimalFormat("000.00");

    public final int numerator;
    public final int denominator;

    private Ratio(int numerator, int denominator) {
        this.numerator = numerator;
        this.denominator = denominator;
    }

    public static Ratio create(int numerator, int denominator) {
        return new Ratio(numerator, denominator);
    }

    /**
     * Gets "x/y" representation.
     */
    public String toString() {
        return numerator + "/" + denominator;
    }

    /**
     * Gets the percentage in integer.
     */
    public int getPercentage() {
        return Math.round(getPercentageFloat());
    }

    /**
     * Gets the percentage in float.
     * For exceptional cases of 0/0, return 100% as it corresponds to expected amount.
     * For error cases of x/0, return 0% as x is unexpected amount.
     */
    public float getPercentageFloat() {
        return denominator == 0 ? (numerator == 0 ? 100.0f : 0.0f) : (100.0f * numerator / denominator);
    }

    /**
     * Gets the percentage as a formatted string used for sorting the html table
     */
    public String getPercentageString() {
      return DATA_FORMAT.format(getPercentageFloat());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Ratio ratio = (Ratio) o;

        return ratio.denominator == denominator &&
                ratio.numerator == numerator;

    }

    @Override
    public int hashCode() {
        int result = numerator;
        result = 31 * result + denominator;
        return result;
    }
}
