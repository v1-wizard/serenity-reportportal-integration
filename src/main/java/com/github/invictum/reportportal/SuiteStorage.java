package com.github.invictum.reportportal;

import io.reactivex.Maybe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Abstraction used to control active suites lifecycle
 */
public class SuiteStorage {

    private static final Logger LOG = LoggerFactory.getLogger(SuiteStorage.class);

    private final ConcurrentHashMap<String, SuiteMetadata> suites = new ConcurrentHashMap<>();

    /**
     * Starts a new suite entity
     *
     * @param id    defined by caller
     * @param start function to start suite
     * @return internal RP identifier associated with created suite
     */
    public Maybe<String> start(String id, Supplier<Maybe<String>> start) {
        suites.computeIfAbsent(id, old -> {
            SuiteMetadata metadata = new SuiteMetadata();
            metadata.id = start.get();
            return metadata;
        });
        SuiteMetadata meta = suites.get(id);
        return meta.id;
    }

    /**
     * Registers a suite finisher, a that should be executed to complete suite
     *
     * @param id       defined by user to finish suite for
     * @param finisher execution logic
     */
    public void suiteFinisher(String id, Runnable finisher) {
        suites.computeIfPresent(id, (key, meta) -> {
            meta.finisher = finisher;
            return meta;
        });
    }

    /**
     * Finishes all active suites without pending retry markers.
     * Executes finishers registered by suiteFinisher method
     */
    public void finalizeActive() {
        finalizeSuites(false);
    }

    /**
     * Finishes all suites that are still open, even if retry markers remain.
     * Used during JVM shutdown, when no further retry callback can arrive in this fork.
     */
    public void finalizeRemaining() {
        finalizeSuites(true);
    }

    private void finalizeSuites(boolean force) {
        suites.forEach((id, meta) -> {
            int pendingRetries = meta.failedTests.size();
            if (!force && pendingRetries > 0) {
                return;
            }
            if (suites.remove(id, meta)) {
                Runnable finisher = meta.finisher;
                if (finisher == null) {
                    LOG.warn("Suite '{}' cannot be finalized because no finisher was registered", id);
                    return;
                }
                if (force && pendingRetries > 0) {
                    LOG.debug("Finalizing suite '{}' with {} unresolved retry marker(s)", id, pendingRetries);
                }
                finisher.run();
            }
        });
    }

    /**
     * Register id of failed test for specific suite by id. Should be called if test failed first time.
     * Part of retries logic.
     *
     * @param suiteId id of suite where fail test was detected
     * @param testId  id of failed test
     */
    public void addNewFail(String suiteId, String testId) {
        SuiteMetadata meta = suites.get(suiteId);
        meta.failedTests.put(testId, new AtomicInteger(0));
    }

    /**
     * This method need for check is test is retry. Part of retries logic.
     *
     * @param suiteId id of suite where fail test was detected
     * @param testId  id of failed test
     */
    public boolean isFailPresent(String suiteId, String testId) {
        SuiteMetadata meta = suites.get(suiteId);
        return meta.failedTests.containsKey(testId);
    }

    /**
     * Should be called if failed test start passed after retry. Part of retries logic.
     *
     * @param suiteId id of suite where fail test was detected
     * @param testId  id of failed test
     */
    public void removeFail(String suiteId, String testId) {
        SuiteMetadata meta = suites.get(suiteId);
        meta.failedTests.remove(testId);
    }

    /**
     * Increase count of failed test to track when exceed count of retires.
     *
     * @param suiteId id of suite where fail test was detected
     * @param testId  id of failed test
     * @return failCount count of retires after ++
     */
    public int incrementAndGetRetriesCount(String suiteId, String testId) {
        SuiteMetadata meta = suites.get(suiteId);
        AtomicInteger failCount = meta.failedTests.get(testId);
        return failCount.incrementAndGet();
    }

    /**
     * Node class that holds suite metadata
     */
    private static class SuiteMetadata {
        private Maybe<String> id;
        private Runnable finisher;
        private final Map<String, AtomicInteger> failedTests = new ConcurrentHashMap<>();
    }
}
