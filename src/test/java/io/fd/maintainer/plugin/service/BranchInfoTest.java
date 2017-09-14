package io.fd.maintainer.plugin.service;

import io.fd.maintainer.plugin.service.SettingsProvider.BranchInfo;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class BranchInfoTest {

    private BranchInfo stableWildcardedInfo;
    private BranchInfo masterInfo;
    private BranchInfo stableInfo;
    private BranchInfo nonReviewBranch;

    @Before
    public void init() {
        this.masterInfo = new BranchInfo("refs/heads/master");
        this.stableInfo = new BranchInfo("refs/heads/stable/1707");
        this.stableWildcardedInfo = new BranchInfo("refs/heads/stable/*");
        this.nonReviewBranch = new BranchInfo("non/review/branch");
    }

    @Test
    public void testNonReview() {
        assertEquals("non/review/branch", nonReviewBranch.getBranchPart());
        assertEquals("non/review/branch", nonReviewBranch.getFullBranchName());
        assertFalse(nonReviewBranch.isGerritReviewBranch());
        assertFalse(nonReviewBranch.isWildcarded());
    }

    @Test
    public void testMasterProcessing() {
        assertEquals("master", masterInfo.getBranchPart());
        assertEquals("refs/heads/master", masterInfo.getFullBranchName());
        assertFalse(masterInfo.isWildcarded());
        assertTrue(masterInfo.isGerritReviewBranch());
    }

    @Test
    public void testStableProcessing() {
        assertEquals("stable/1707", stableInfo.getBranchPart());
        assertEquals("refs/heads/stable/1707", stableInfo.getFullBranchName());
        assertFalse(stableInfo.isWildcarded());
        assertTrue(stableInfo.isGerritReviewBranch());
    }

    @Test
    public void testWildcaded() {
        assertEquals("stable", stableWildcardedInfo.getBranchPart());
        assertEquals("refs/heads/stable/*", stableWildcardedInfo.getFullBranchName());
        assertTrue(stableWildcardedInfo.isWildcarded());
        assertTrue(stableWildcardedInfo.isGerritReviewBranch());
    }

    @Test
    public void testCompare() {
        assertFalse(masterInfo.isAlternativeFor(stableInfo));
        assertFalse(masterInfo.isAlternativeFor(stableWildcardedInfo));
        assertTrue(masterInfo.isAlternativeFor(masterInfo));
        assertFalse(masterInfo.isAlternativeFor(nonReviewBranch));

        assertFalse(stableInfo.isAlternativeFor(masterInfo));
        assertTrue(stableInfo.isAlternativeFor(stableWildcardedInfo));
        assertTrue(stableInfo.isAlternativeFor(stableInfo));
        assertFalse(stableInfo.isAlternativeFor(nonReviewBranch));

        assertFalse(stableWildcardedInfo.isAlternativeFor(masterInfo));
        assertTrue(stableWildcardedInfo.isAlternativeFor(stableInfo));
        assertTrue(stableWildcardedInfo.isAlternativeFor(stableWildcardedInfo));
        assertFalse(stableWildcardedInfo.isAlternativeFor(nonReviewBranch));

        assertFalse(nonReviewBranch.isAlternativeFor(masterInfo));
        assertFalse(nonReviewBranch.isAlternativeFor(stableInfo));
        assertFalse(nonReviewBranch.isAlternativeFor(stableWildcardedInfo));
        assertTrue(nonReviewBranch.isAlternativeFor(nonReviewBranch));
    }

}