package com.lingolive.app;

import org.junit.Test;
import static org.junit.Assert.*;

public class TranslationRevisionTest {
    @Test public void latePartialCannotOverwriteNewerFinal() {
        TranslationRevision revisions = new TranslationRevision();
        long partial = revisions.next();
        long finalResult = revisions.next();
        assertFalse(revisions.isCurrent(partial));
        assertTrue(revisions.isCurrent(finalResult));
    }
    @Test public void languageChangeInvalidatesPendingTranslation() {
        TranslationRevision revisions = new TranslationRevision();
        long previousLanguage = revisions.next();
        revisions.next();
        assertFalse(revisions.isCurrent(previousLanguage));
    }
}
