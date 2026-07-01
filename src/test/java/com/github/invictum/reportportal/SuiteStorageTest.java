package com.github.invictum.reportportal;

import io.reactivex.Maybe;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

public class SuiteStorageTest {

    SuiteStorage storage;

    @Before
    public void setupStorage() {
        storage = new SuiteStorage();
        storage.start("suite", Maybe::empty);
    }


    @Test
    public void testAddNewFail() {
        storage.addNewFail("suite", "storage");
        Assert.assertTrue(storage.isFailPresent("suite", "storage"));
    }

    @Test
    public void testIncrementRetiresCount() {
        storage.addNewFail("suite", "storage");
        Assert.assertEquals(1, storage.incrementAndGetRetriesCount("suite", "storage"));
        Assert.assertEquals(2, storage.incrementAndGetRetriesCount("suite", "storage"));
        Assert.assertEquals(3, storage.incrementAndGetRetriesCount("suite", "storage"));
    }

    @Test
    public void finalizeActiveShouldFinishSuiteWithoutPendingRetries() {
        AtomicInteger finishes = new AtomicInteger();
        storage.suiteFinisher("suite", finishes::incrementAndGet);

        storage.finalizeActive();
        storage.finalizeActive();

        Assert.assertEquals(1, finishes.get());
    }

    @Test
    public void finalizeActiveShouldSkipSuiteWithPendingRetries() {
        AtomicInteger finishes = new AtomicInteger();
        storage.suiteFinisher("suite", finishes::incrementAndGet);
        storage.addNewFail("suite", "test");

        storage.finalizeActive();

        Assert.assertEquals(0, finishes.get());
        Assert.assertTrue(storage.isFailPresent("suite", "test"));
    }

    @Test
    public void finalizeRemainingShouldFinishSuiteWithPendingRetries() {
        AtomicInteger finishes = new AtomicInteger();
        storage.suiteFinisher("suite", finishes::incrementAndGet);
        storage.addNewFail("suite", "test");

        storage.finalizeRemaining();
        storage.finalizeRemaining();

        Assert.assertEquals(1, finishes.get());
    }

}
