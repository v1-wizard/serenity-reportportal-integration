# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Single-module Maven library that integrates [Serenity BDD](https://serenity-bdd.github.io/) with [Report Portal](https://reportportal.io). Built as a `jar` that downstream test projects depend on — it has no runnable entry point. Java 21, `groupId: com.github.invictum`, `version: 3.6-wf` (the `-wf` suffix marks this as a fork divergent from upstream Invictum releases).

## Build & test

```bash
mvn clean install   # full build (incl. tests)
mvn test            # unit tests only
mvn -Dtest=RegularTest test                    # single class
mvn -Dtest=RegularTest#methodName test         # single method
```

Tests are plain JUnit 4 + Mockito; there is no Serenity/Report Portal endpoint required to run them. Note: `.github/workflows/junit-test.yml` still pins JDK 8 even though the pom targets Java 21 — CI is stale and will fail on push; fix the workflow if you need green CI.

## Architecture

This library is wired into Serenity's listener lifecycle via Java SPI, then takes over the translation from Serenity domain objects to Report Portal API calls. Reading these together gives the full picture; reading any one in isolation will not.

**SPI entry point.** `src/main/resources/META-INF/services/net.thucydides.model.steps.StepListener` registers `ReportPortalListener`. Serenity discovers it automatically when the jar is on the classpath — there is no explicit wiring on the consumer side. This is why "just add the dependency" works in the README.

**Listener → recorder boundary.** `ReportPortalListener` implements Serenity's `StepListener` but deliberately leaves most callbacks empty. It only acts on three signals:
- `testFinished(TestOutcome, ...)` — hands the full outcome to `TestRecorder.forTest(...)` which dispatches per-test.
- `testSuiteFinished()` — flushes `SuiteStorage` (suite finishers only fire here, not eagerly).
- `stepFinished` / `stepFailed` — harvest Selenium driver logs when enabled.

The listener does NOT stream step events to Report Portal as they happen. Reporting is deferred until the test finishes and the whole `TestOutcome` tree is replayed in `proceedSteps`. Any change that assumes "step-start = RP-step-start" will break.

**Recorder dispatch.** `TestRecorder.forTest(outcome)` chooses:
- `BddDataDriven` — only when `outcome.isDataDriven()` AND source is cucumber or jbehave. It treats each data-driven scenario row as the *last* TestStep and reports only that step (this is why parametrized Serenity tests can't run concurrently — README "Limitations").
- `Regular` — everything else (JUnit, non-data-driven BDD). Also handles retries (see `processRetries`) which depend on the `failsafe.rerunFailingTestsCount` / `surefire.rerunFailingTestsCount` system properties read by `ReportIntegrationConfig.retriesCount()`.

**Log units = pluggable per-step transforms.** A log unit is a `Function<TestStep, Collection<SaveLogRQ>>`. `LogsPreset` (DEFAULT/FULL/CUSTOM enum) holds them; `LogUnitsHolder.proceed(step)` runs each unit and emits all returned `SaveLogRQ`s via `ReportPortal.emitLog`. Built-in units live in `log/unit/` (`Attachment`, `Error`, `Rest`, `Selenium`). When adding a unit, set `setLogTime` and `setLevel` correctly — Report Portal silently drops logs whose timestamp falls outside the active test window.

**Guice singletons.** `IntegrationInjector` is a static double-checked-locked `Injector`. `SerenityPortalModule` binds `Launch`, `LogUnitsHolder`, `LogStorage`, `SuiteStorage` as singletons. `Launch` is bound `asEagerSingleton`, meaning `ReportLaunchProvider.get()` runs the *first* time the injector is built — this is what triggers `launch.start()` and registers the JVM shutdown hook that finishes the launch and (optionally) merges sub-module launches.

**Configuration.** `ReportIntegrationConfig.get()` is the singleton config surface. The README is the canonical reference for what is configurable; the important wrinkle for code work is that *all* config must be set before the first call into `IntegrationInjector.getInjector()` because of the eager `Launch` binding. After tests start, mutations are ignored.

**Multi-module merge.** Two system properties drive it: `serenity.rp.communication.dir` (a shared filesystem dir used as a coordination rendezvous) and `serenity.rp.modules.count` (positive int > 1). Each sub-module writes its launch ID to a file in the shared dir on shutdown; when the file count equals `modules.count`, the last process to finish performs the merge. The shared dir is wiped at the end — never point it at a directory with real data.

**Retry semantics.** Tracked per (suiteId, testId) inside `SuiteStorage.failedTests`. The first failure is recorded; subsequent runs of the same test are marked `.withRetry()` on the RP item. Retry tracking only activates when `retriesCount()` > 0 (i.e. the failsafe/surefire system property is set).