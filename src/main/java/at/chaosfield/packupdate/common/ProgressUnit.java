package at.chaosfield.packupdate.common;

public enum ProgressUnit {
    Scalar,
    Percent,
    Bytes;

    private static final int Kibibyte = 1024;
    private static final int Mebibyte = Kibibyte * 1024;
    private static final int Gibibyte = Mebibyte * 1024;

    public String render(int number) {
        switch (this) {
            case Scalar:
                return "" + number;
            case Percent:
                return "" + number + " %";
            case Bytes:
                if (number < Kibibyte) {
                    return "" + number + " B";
                } else if (number < Mebibyte) {
                    return String.format("%.2f KiB", ((float) number) / Kibibyte);
                } else if (number < Gibibyte) {
                    return String.format("%.2f MiB", ((float) number) / Mebibyte);
                } else {
                    return String.format("%.2f GiB", ((float) number) / Gibibyte);
                }
            default:
                throw new RuntimeException("Unreachable code");
        }
    }
}
