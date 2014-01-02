package com.danosipov.asyncandroid.rxdownloader.events;

public class DownloadProgressEvent {
    private long loaded;
    private long total;

    public DownloadProgressEvent(long loaded, long total) {
        this.loaded = loaded;
        this.total = total;
    }

    public long getLoaded() {
        return loaded;
    }

    public String getLoadedBytes() {
        return humanReadableByteCount(loaded, true);
    }

    public long getTotal() {
        return total;
    }

    public String getTotalBytes() {
        return humanReadableByteCount(total, true);
    }

    public double getProgress() {
        return ((double) (loaded * 100)) / total;
    }


    /**
     * Thanks to helpful StackOverflow answer!
     * http://stackoverflow.com/questions/3758606/how-to-convert-byte-size-into-human-readable-format-in-java
     */
    public static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }
}
