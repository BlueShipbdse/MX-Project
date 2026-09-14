package kireiko.dev.millennium.ml.logic.rnn.data;

public record SequenceData(double[][] x, double[] mask) {

    public int length() {
        return x.length;
    }
}
