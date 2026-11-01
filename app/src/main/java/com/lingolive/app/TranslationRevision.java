package com.lingolive.app;

/** Rejects asynchronous results from an older utterance or language pair. */
final class TranslationRevision {
    private long revision;
    long next() { return ++revision; }
    boolean isCurrent(long candidate) { return revision == candidate; }
}
