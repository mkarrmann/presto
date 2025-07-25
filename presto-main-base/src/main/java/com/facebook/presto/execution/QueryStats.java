/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.presto.execution;

import com.facebook.presto.common.RuntimeStats;
import com.facebook.presto.operator.BlockedReason;
import com.facebook.presto.operator.ExchangeOperator;
import com.facebook.presto.operator.MergeOperator;
import com.facebook.presto.operator.OperatorStats;
import com.facebook.presto.operator.ScanFilterAndProjectOperator;
import com.facebook.presto.operator.TableScanOperator;
import com.facebook.presto.operator.TableWriterOperator;
import com.facebook.presto.spi.eventlistener.StageGcStatistics;
import com.facebook.presto.sql.planner.PlanFragment;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.airlift.units.DataSize;
import io.airlift.units.Duration;
import org.joda.time.DateTime;

import javax.annotation.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;

import static com.facebook.presto.util.DateTimeUtils.toTimeStampInMillis;
import static com.google.common.base.Preconditions.checkArgument;
import static io.airlift.units.DataSize.succinctBytes;
import static io.airlift.units.Duration.succinctDuration;
import static java.lang.Math.min;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class QueryStats
{
    private final long createTimeInMillis;

    private final long executionStartTimeInMillis;
    private final long lastHeartbeatInMillis;
    private final long endTimeInMillis;

    private final Duration elapsedTime;
    private final Duration waitingForPrerequisitesTime;
    private final Duration queuedTime;
    private final Duration resourceWaitingTime;
    private final Duration semanticAnalyzingTime;
    private final Duration columnAccessPermissionCheckingTime;
    private final Duration dispatchingTime;
    private final Duration executionTime;
    private final Duration analysisTime;
    private final Duration totalPlanningTime;
    private final Duration finishingTime;

    private final int totalTasks;
    private final int runningTasks;
    private final int peakRunningTasks;
    private final int completedTasks;

    private final int totalDrivers;
    private final int queuedDrivers;
    private final int runningDrivers;
    private final int blockedDrivers;
    private final int completedDrivers;

    private final int totalNewDrivers;
    private final int queuedNewDrivers;
    private final int runningNewDrivers;
    private final int completedNewDrivers;

    private final int totalSplits;
    private final int queuedSplits;
    private final int runningSplits;
    private final int completedSplits;

    private final double cumulativeUserMemory;
    private final double cumulativeTotalMemory;
    private final DataSize userMemoryReservation;
    private final DataSize totalMemoryReservation;
    private final DataSize peakUserMemoryReservation;
    private final DataSize peakTotalMemoryReservation;
    private final DataSize peakTaskTotalMemory;
    private final DataSize peakTaskUserMemory;
    private final DataSize peakNodeTotalMemory;

    private final boolean scheduled;
    private final Duration totalScheduledTime;
    private final Duration totalCpuTime;
    private final Duration retriedCpuTime;
    private final Duration totalBlockedTime;
    private final boolean fullyBlocked;
    private final Set<BlockedReason> blockedReasons;

    private final DataSize totalAllocation;

    private final DataSize rawInputDataSize;
    private final long rawInputPositions;

    private final DataSize processedInputDataSize;
    private final long processedInputPositions;

    private final DataSize shuffledDataSize;
    private final long shuffledPositions;

    private final DataSize outputDataSize;
    private final long outputPositions;

    private final long writtenOutputPositions;
    private final DataSize writtenOutputLogicalDataSize;
    private final DataSize writtenOutputPhysicalDataSize;

    private final DataSize writtenIntermediatePhysicalDataSize;

    private final List<StageGcStatistics> stageGcStatistics;

    private final List<OperatorStats> operatorSummaries;

    // RuntimeStats aggregated at the query level including the metrics exposed in every task and every operator.
    private final RuntimeStats runtimeStats;

    public QueryStats(
            long createTimeInMillis,
            long executionStartTimeInMillis,
            long lastHeartbeatInMillis,
            long endTimeInMillis,

            Duration elapsedTime,
            Duration waitingForPrerequisitesTime,
            Duration queuedTime,
            Duration resourceWaitingTime,
            Duration semanticAnalyzingTime,
            Duration columnAccessPermissionCheckingTime,
            Duration dispatchingTime,
            Duration executionTime,
            Duration analysisTime,
            Duration totalPlanningTime,
            Duration finishingTime,

            int totalTasks,
            int runningTasks,
            int peakRunningTasks,
            int completedTasks,

            int totalDrivers,
            int queuedDrivers,
            int runningDrivers,
            int blockedDrivers,
            int completedDrivers,

            int totalNewDrivers,
            int queuedNewDrivers,
            int runningNewDrivers,
            int completedNewDrivers,

            int totalSplits,
            int queuedSplits,
            int runningSplits,
            int completedSplits,

            double cumulativeUserMemory,
            double cumulativeTotalMemory,
            DataSize userMemoryReservation,
            DataSize totalMemoryReservation,
            DataSize peakUserMemoryReservation,
            DataSize peakTotalMemoryReservation,
            DataSize peakTaskUserMemory,
            DataSize peakTaskTotalMemory,
            DataSize peakNodeTotalMemory,

            boolean scheduled,
            Duration totalScheduledTime,
            Duration totalCpuTime,
            Duration retriedCpuTime,
            Duration totalBlockedTime,
            boolean fullyBlocked,
            Set<BlockedReason> blockedReasons,

            DataSize totalAllocation,

            DataSize rawInputDataSize,
            long rawInputPositions,

            DataSize processedInputDataSize,
            long processedInputPositions,

            DataSize shuffledDataSize,
            long shuffledPositions,

            DataSize outputDataSize,
            long outputPositions,

            long writtenOutputPositions,
            DataSize writtenOutputLogicalDataSize,
            DataSize writtenOutputPhysicalDataSize,

            DataSize writtenIntermediatePhysicalDataSize,

            List<StageGcStatistics> stageGcStatistics,

            List<OperatorStats> operatorSummaries,

            RuntimeStats runtimeStats)
    {
        checkArgument(createTimeInMillis >= 0, "createTimeInMillis is negative");
        this.createTimeInMillis = createTimeInMillis;
        this.executionStartTimeInMillis = executionStartTimeInMillis;
        checkArgument(lastHeartbeatInMillis >= 0, "lastHeartbeatInMillis is negative");
        this.lastHeartbeatInMillis = lastHeartbeatInMillis;
        this.endTimeInMillis = endTimeInMillis;

        this.elapsedTime = requireNonNull(elapsedTime, "elapsedTime is null");
        this.waitingForPrerequisitesTime = requireNonNull(waitingForPrerequisitesTime, "waitingForPrerequisitesTime is null");
        this.queuedTime = requireNonNull(queuedTime, "queuedTime is null");
        this.resourceWaitingTime = requireNonNull(resourceWaitingTime, "resourceWaitingTime is null");
        this.semanticAnalyzingTime = requireNonNull(semanticAnalyzingTime, "semanticAnalyzingTime is null");
        this.columnAccessPermissionCheckingTime = requireNonNull(columnAccessPermissionCheckingTime, "columnAccessPermissionCheckingTime is null");
        this.dispatchingTime = requireNonNull(dispatchingTime, "dispatchingTime is null");
        this.executionTime = requireNonNull(executionTime, "executionTime is null");
        this.analysisTime = requireNonNull(analysisTime, "analysisTime is null");
        this.totalPlanningTime = requireNonNull(totalPlanningTime, "totalPlanningTime is null");
        this.finishingTime = requireNonNull(finishingTime, "finishingTime is null");

        checkArgument(totalTasks >= 0, "totalTasks is negative");
        this.totalTasks = totalTasks;
        checkArgument(runningTasks >= 0, "runningTasks is negative");
        this.runningTasks = runningTasks;
        checkArgument(peakRunningTasks >= 0, "peakRunningTasks is negative");
        this.peakRunningTasks = peakRunningTasks;
        checkArgument(completedTasks >= 0, "completedTasks is negative");
        this.completedTasks = completedTasks;

        checkArgument(totalDrivers >= 0, "totalDrivers is negative");
        this.totalDrivers = totalDrivers;
        checkArgument(queuedDrivers >= 0, "queuedDrivers is negative");
        this.queuedDrivers = queuedDrivers;
        checkArgument(runningDrivers >= 0, "runningDrivers is negative");
        this.runningDrivers = runningDrivers;
        checkArgument(blockedDrivers >= 0, "blockedDrivers is negative");
        this.blockedDrivers = blockedDrivers;
        checkArgument(completedDrivers >= 0, "completedDrivers is negative");
        this.completedDrivers = completedDrivers;
        checkArgument(totalNewDrivers >= 0, "totalNewDrivers is negative");
        this.totalNewDrivers = totalNewDrivers;
        checkArgument(queuedNewDrivers >= 0, "queuedNewDrivers is negative");
        this.queuedNewDrivers = queuedNewDrivers;
        checkArgument(runningNewDrivers >= 0, "runningNewDrivers is negative");
        this.runningNewDrivers = runningNewDrivers;
        checkArgument(completedNewDrivers >= 0, "completedNewDrivers is negative");
        this.completedNewDrivers = completedNewDrivers;
        checkArgument(totalSplits >= 0, "totalSplits is negative");
        this.totalSplits = totalSplits;
        checkArgument(queuedSplits >= 0, "queuedSplits is negative");
        this.queuedSplits = queuedSplits;
        checkArgument(runningSplits >= 0, "runningSplits is negative");
        this.runningSplits = runningSplits;
        checkArgument(completedSplits >= 0, "completedSplits is negative");
        this.completedSplits = completedSplits;
        checkArgument(cumulativeUserMemory >= 0, "cumulativeUserMemory is negative");
        this.cumulativeUserMemory = cumulativeUserMemory;
        checkArgument(cumulativeTotalMemory >= 0, "cumulativeTotalMemory is negative");
        this.cumulativeTotalMemory = cumulativeTotalMemory;
        this.userMemoryReservation = requireNonNull(userMemoryReservation, "userMemoryReservation is null");
        this.totalMemoryReservation = requireNonNull(totalMemoryReservation, "totalMemoryReservation is null");
        this.peakUserMemoryReservation = requireNonNull(peakUserMemoryReservation, "peakUserMemoryReservation is null");
        this.peakTotalMemoryReservation = requireNonNull(peakTotalMemoryReservation, "peakTotalMemoryReservation is null");
        this.peakTaskTotalMemory = requireNonNull(peakTaskTotalMemory, "peakTaskTotalMemory is null");
        this.peakTaskUserMemory = requireNonNull(peakTaskUserMemory, "peakTaskUserMemory is null");
        this.peakNodeTotalMemory = requireNonNull(peakNodeTotalMemory, "peakNodeTotalMemory is null");
        this.scheduled = scheduled;
        this.totalScheduledTime = requireNonNull(totalScheduledTime, "totalScheduledTime is null");
        this.totalCpuTime = requireNonNull(totalCpuTime, "totalCpuTime is null");
        this.retriedCpuTime = requireNonNull(retriedCpuTime, "totalCpuTime is null");
        this.totalBlockedTime = requireNonNull(totalBlockedTime, "totalBlockedTime is null");
        this.fullyBlocked = fullyBlocked;
        this.blockedReasons = ImmutableSet.copyOf(requireNonNull(blockedReasons, "blockedReasons is null"));

        this.totalAllocation = requireNonNull(totalAllocation, "totalAllocation is null");

        this.rawInputDataSize = requireNonNull(rawInputDataSize, "rawInputDataSize is null");
        checkArgument(rawInputPositions >= 0, "rawInputPositions is negative");
        this.rawInputPositions = rawInputPositions;

        this.processedInputDataSize = requireNonNull(processedInputDataSize, "processedInputDataSize is null");
        checkArgument(processedInputPositions >= 0, "processedInputPositions is negative");
        this.processedInputPositions = processedInputPositions;

        this.shuffledDataSize = requireNonNull(shuffledDataSize, "shuffledDataSize is null");
        checkArgument(shuffledPositions >= 0, "shuffledPositions is negative");
        this.shuffledPositions = shuffledPositions;

        this.outputDataSize = requireNonNull(outputDataSize, "outputDataSize is null");
        checkArgument(outputPositions >= 0, "outputPositions is negative");
        this.outputPositions = outputPositions;

        checkArgument(writtenOutputPositions >= 0, "writtenOutputPositions is negative: %s", writtenOutputPositions);
        this.writtenOutputPositions = writtenOutputPositions;
        this.writtenOutputLogicalDataSize = requireNonNull(writtenOutputLogicalDataSize, "writtenOutputLogicalDataSize is null");
        this.writtenOutputPhysicalDataSize = requireNonNull(writtenOutputPhysicalDataSize, "writtenOutputPhysicalDataSize is null");
        this.writtenIntermediatePhysicalDataSize = requireNonNull(writtenIntermediatePhysicalDataSize, "writtenIntermediatePhysicalDataSize is null");

        this.stageGcStatistics = ImmutableList.copyOf(requireNonNull(stageGcStatistics, "stageGcStatistics is null"));

        this.operatorSummaries = ImmutableList.copyOf(requireNonNull(operatorSummaries, "operatorSummaries is null"));

        this.runtimeStats = (runtimeStats == null) ? new RuntimeStats() : runtimeStats;
    }

    @JsonCreator
    public QueryStats(
            @JsonProperty("createTime") DateTime createTime,
            @JsonProperty("executionStartTime") DateTime executionStartTime,
            @JsonProperty("lastHeartbeat") DateTime lastHeartbeat,
            @JsonProperty("endTime") DateTime endTime,

            @JsonProperty("elapsedTime") Duration elapsedTime,
            @JsonProperty("waitingForPrerequisitesTime") Duration waitingForPrerequisitesTime,
            @JsonProperty("queuedTime") Duration queuedTime,
            @JsonProperty("resourceWaitingTime") Duration resourceWaitingTime,
            @JsonProperty("semanticAnalyzingTime") Duration semanticAnalyzingTime,
            @JsonProperty("columnAccessPermissionCheckingTime") Duration columnAccessPermissionCheckingTime,
            @JsonProperty("dispatchingTime") Duration dispatchingTime,
            @JsonProperty("executionTime") Duration executionTime,
            @JsonProperty("analysisTime") Duration analysisTime,
            @JsonProperty("totalPlanningTime") Duration totalPlanningTime,
            @JsonProperty("finishingTime") Duration finishingTime,

            @JsonProperty("totalTasks") int totalTasks,
            @JsonProperty("runningTasks") int runningTasks,
            @JsonProperty("peakRunningTasks") int peakRunningTasks,
            @JsonProperty("completedTasks") int completedTasks,

            @JsonProperty("totalDrivers") int totalDrivers,
            @JsonProperty("queuedDrivers") int queuedDrivers,
            @JsonProperty("runningDrivers") int runningDrivers,
            @JsonProperty("blockedDrivers") int blockedDrivers,
            @JsonProperty("completedDrivers") int completedDrivers,

            @JsonProperty("totalNewDrivers") int totalNewDrivers,
            @JsonProperty("queuedNewDrivers") int queuedNewDrivers,
            @JsonProperty("runningNewDrivers") int runningNewDrivers,
            @JsonProperty("completedNewDrivers") int completedNewDrivers,

            @JsonProperty("totalSplits") int totalSplits,
            @JsonProperty("queuedSplits") int queuedSplits,
            @JsonProperty("runningSplits") int runningSplits,
            @JsonProperty("completedSplits") int completedSplits,

            @JsonProperty("cumulativeUserMemory") double cumulativeUserMemory,
            @JsonProperty("cumulativeTotalMemory") double cumulativeTotalMemory,
            @JsonProperty("userMemoryReservation") DataSize userMemoryReservation,
            @JsonProperty("totalMemoryReservation") DataSize totalMemoryReservation,
            @JsonProperty("peakUserMemoryReservation") DataSize peakUserMemoryReservation,
            @JsonProperty("peakTotalMemoryReservation") DataSize peakTotalMemoryReservation,
            @JsonProperty("peakTaskUserMemory") DataSize peakTaskUserMemory,
            @JsonProperty("peakTaskTotalMemory") DataSize peakTaskTotalMemory,
            @JsonProperty("peakNodeTotalMemory") DataSize peakNodeTotalMemory,

            @JsonProperty("scheduled") boolean scheduled,
            @JsonProperty("totalScheduledTime") Duration totalScheduledTime,
            @JsonProperty("totalCpuTime") Duration totalCpuTime,
            @JsonProperty("retriedCpuTime") Duration retriedCpuTime,
            @JsonProperty("totalBlockedTime") Duration totalBlockedTime,
            @JsonProperty("fullyBlocked") boolean fullyBlocked,
            @JsonProperty("blockedReasons") Set<BlockedReason> blockedReasons,

            @JsonProperty("totalAllocation") DataSize totalAllocation,

            @JsonProperty("rawInputDataSize") DataSize rawInputDataSize,
            @JsonProperty("rawInputPositions") long rawInputPositions,

            @JsonProperty("processedInputDataSize") DataSize processedInputDataSize,
            @JsonProperty("processedInputPositions") long processedInputPositions,

            @JsonProperty("shuffledDataSize") DataSize shuffledDataSize,
            @JsonProperty("shuffledPositions") long shuffledPositions,

            @JsonProperty("outputDataSize") DataSize outputDataSize,
            @JsonProperty("outputPositions") long outputPositions,

            @JsonProperty("writtenOutputPositions") long writtenOutputPositions,
            @JsonProperty("writtenOutputLogicalDataSize") DataSize writtenOutputLogicalDataSize,
            @JsonProperty("writtenOutputPhysicalDataSize") DataSize writtenOutputPhysicalDataSize,

            @JsonProperty("writtenIntermediatePhysicalDataSize") DataSize writtenIntermediatePhysicalDataSize,

            @JsonProperty("stageGcStatistics") List<StageGcStatistics> stageGcStatistics,

            @JsonProperty("operatorSummaries") List<OperatorStats> operatorSummaries,

            @JsonProperty("runtimeStats") RuntimeStats runtimeStats)
    {
        this(toTimeStampInMillis(createTime),
                toTimeStampInMillis(executionStartTime),
                toTimeStampInMillis(lastHeartbeat),
                toTimeStampInMillis(endTime),

                elapsedTime,
                waitingForPrerequisitesTime,
                queuedTime,
                resourceWaitingTime,
                semanticAnalyzingTime,
                columnAccessPermissionCheckingTime,
                dispatchingTime,
                executionTime,
                analysisTime,
                totalPlanningTime,
                finishingTime,

                totalTasks,
                runningTasks,
                peakRunningTasks,
                completedTasks,

                totalDrivers,
                queuedDrivers,
                runningDrivers,
                blockedDrivers,
                completedDrivers,

                totalNewDrivers,
                queuedNewDrivers,
                runningNewDrivers,
                completedNewDrivers,

                totalSplits,
                queuedSplits,
                runningSplits,
                completedSplits,

                cumulativeUserMemory,
                cumulativeTotalMemory,
                userMemoryReservation,
                totalMemoryReservation,
                peakUserMemoryReservation,
                peakTotalMemoryReservation,
                peakTaskUserMemory,
                peakTaskTotalMemory,
                peakNodeTotalMemory,

                scheduled,
                totalScheduledTime,
                totalCpuTime,
                retriedCpuTime,
                totalBlockedTime,
                fullyBlocked,
                blockedReasons,

                totalAllocation,

                rawInputDataSize,
                rawInputPositions,

                processedInputDataSize,
                processedInputPositions,

                shuffledDataSize,
                shuffledPositions,

                outputDataSize,
                outputPositions,

                writtenOutputPositions,
                writtenOutputLogicalDataSize,
                writtenOutputPhysicalDataSize,

                writtenIntermediatePhysicalDataSize,

                stageGcStatistics,
                operatorSummaries,

                runtimeStats);
    }

    public static QueryStats create(
            QueryStateTimer queryStateTimer,
            Optional<StageInfo> rootStage,
            List<StageInfo> allStages,
            int peakRunningTasks,
            long peakUserMemoryReservation,
            long peakTotalMemoryReservation,
            long peakTaskUserMemory,
            long peakTaskTotalMemory,
            long peakNodeTotalMemory,
            RuntimeStats runtimeStats)
    {
        int totalTasks = 0;
        int runningTasks = 0;
        int completedTasks = 0;

        int totalDrivers = 0;
        int queuedDrivers = 0;
        int runningDrivers = 0;
        int blockedDrivers = 0;
        int completedDrivers = 0;

        int totalNewDrivers = 0;
        int queuedNewDrivers = 0;
        int runningNewDrivers = 0;
        int completedNewDrivers = 0;

        int totalSplits = 0;
        int queuedSplits = 0;
        int runningSplits = 0;
        int completedSplits = 0;

        double cumulativeUserMemory = 0;
        double cumulativeTotalMemory = 0;
        long userMemoryReservation = 0;
        long totalMemoryReservation = 0;

        long totalScheduledTime = 0;
        long totalCpuTime = 0;
        long retriedCpuTime = 0;
        long totalBlockedTime = 0;

        long totalAllocation = 0;

        long rawInputDataSize = 0;
        long rawInputPositions = 0;

        long processedInputDataSize = 0;
        long processedInputPositions = 0;

        long shuffledDataSize = 0;
        long shuffledPositions = 0;

        long outputDataSize = 0;
        long outputPositions = 0;

        long writtenOutputPositions = 0;
        long writtenOutputLogicalDataSize = 0;
        long writtenOutputPhysicalDataSize = 0;

        long writtenIntermediatePhysicalDataSize = 0;

        ImmutableList.Builder<StageGcStatistics> stageGcStatistics = ImmutableList.builderWithExpectedSize(allStages.size());

        boolean fullyBlocked = rootStage.isPresent();
        Set<BlockedReason> blockedReasons = new HashSet<>();

        ImmutableList.Builder<OperatorStats> operatorStatsSummary = ImmutableList.builder();
        RuntimeStats mergedRuntimeStats = RuntimeStats.copyOf(runtimeStats);
        for (StageInfo stageInfo : allStages) {
            StageExecutionStats stageExecutionStats = stageInfo.getLatestAttemptExecutionInfo().getStats();
            totalTasks += stageExecutionStats.getTotalTasks();
            runningTasks += stageExecutionStats.getRunningTasks();
            completedTasks += stageExecutionStats.getCompletedTasks();

            totalDrivers += stageExecutionStats.getTotalDrivers();
            queuedDrivers += stageExecutionStats.getQueuedDrivers();
            runningDrivers += stageExecutionStats.getRunningDrivers();
            blockedDrivers += stageExecutionStats.getBlockedDrivers();
            completedDrivers += stageExecutionStats.getCompletedDrivers();

            totalNewDrivers += stageExecutionStats.getTotalNewDrivers();
            queuedNewDrivers += stageExecutionStats.getQueuedNewDrivers();
            runningNewDrivers += stageExecutionStats.getRunningNewDrivers();
            completedNewDrivers += stageExecutionStats.getCompletedNewDrivers();

            totalSplits += stageExecutionStats.getTotalSplits();
            queuedSplits += stageExecutionStats.getQueuedSplits();
            runningSplits += stageExecutionStats.getRunningSplits();
            completedSplits += stageExecutionStats.getCompletedSplits();

            cumulativeUserMemory += stageExecutionStats.getCumulativeUserMemory();
            cumulativeTotalMemory += stageExecutionStats.getCumulativeTotalMemory();
            userMemoryReservation += stageExecutionStats.getUserMemoryReservationInBytes();
            totalMemoryReservation += stageExecutionStats.getTotalMemoryReservationInBytes();
            totalScheduledTime += stageExecutionStats.getTotalScheduledTime().roundTo(MILLISECONDS);
            totalCpuTime += stageExecutionStats.getTotalCpuTime().roundTo(MILLISECONDS);
            retriedCpuTime += computeRetriedCpuTime(stageInfo);
            totalBlockedTime += stageExecutionStats.getTotalBlockedTime().roundTo(MILLISECONDS);
            if (!stageInfo.getLatestAttemptExecutionInfo().getState().isDone()) {
                fullyBlocked &= stageExecutionStats.isFullyBlocked();
                blockedReasons.addAll(stageExecutionStats.getBlockedReasons());
            }

            totalAllocation += stageExecutionStats.getTotalAllocationInBytes();

            if (stageInfo.getPlan().isPresent()) {
                PlanFragment plan = stageInfo.getPlan().get();
                for (OperatorStats operatorStats : stageExecutionStats.getOperatorSummaries()) {
                    // NOTE: we need to literally check each operator type to tell if the source is from table input or shuffled input. A stage can have input from both types of source.
                    String operatorType = operatorStats.getOperatorType();
                    if (operatorType.equals(ExchangeOperator.class.getSimpleName()) || operatorType.equals(MergeOperator.class.getSimpleName())) {
                        shuffledPositions += operatorStats.getRawInputPositions();
                        shuffledDataSize += operatorStats.getRawInputDataSizeInBytes();
                    }
                    else if (operatorType.equals(TableScanOperator.class.getSimpleName()) || operatorType.equals(ScanFilterAndProjectOperator.class.getSimpleName())) {
                        rawInputDataSize += operatorStats.getRawInputDataSizeInBytes();
                        rawInputPositions += operatorStats.getRawInputPositions();
                    }
                }
                processedInputDataSize += stageExecutionStats.getProcessedInputDataSizeInBytes();
                processedInputPositions += stageExecutionStats.getProcessedInputPositions();

                if (plan.isOutputTableWriterFragment()) {
                    writtenOutputPositions += stageExecutionStats.getOperatorSummaries().stream()
                            .filter(stats -> stats.getOperatorType().equals(TableWriterOperator.OPERATOR_TYPE))
                            .mapToLong(OperatorStats::getInputPositions)
                            .sum();
                    writtenOutputLogicalDataSize += stageExecutionStats.getOperatorSummaries().stream()
                            .filter(stats -> stats.getOperatorType().equals(TableWriterOperator.OPERATOR_TYPE))
                            .mapToLong(OperatorStats::getInputDataSizeInBytes)
                            .sum();
                    writtenOutputPhysicalDataSize += stageExecutionStats.getPhysicalWrittenDataSizeInBytes();
                }
                else {
                    writtenIntermediatePhysicalDataSize += stageExecutionStats.getPhysicalWrittenDataSizeInBytes();
                }
            }

            stageGcStatistics.add(stageExecutionStats.getGcInfo());

            operatorStatsSummary.addAll(stageExecutionStats.getOperatorSummaries());
            // We prepend each metric name with the stage id to avoid merging metrics across stages.
            int stageId = stageInfo.getStageId().getId();
            stageExecutionStats.getRuntimeStats().getMetrics().forEach((name, metric) -> {
                String metricName = String.format("S%d-%s", stageId, name);
                mergedRuntimeStats.mergeMetric(metricName, metric);
            });
        }

        if (rootStage.isPresent()) {
            StageExecutionStats outputStageStats = rootStage.get().getLatestAttemptExecutionInfo().getStats();
            outputDataSize += outputStageStats.getOutputDataSizeInBytes();
            outputPositions += outputStageStats.getOutputPositions();
        }

        boolean isScheduled = rootStage.isPresent() && allStages.stream()
                .map(StageInfo::getLatestAttemptExecutionInfo)
                .map(StageExecutionInfo::getState)
                .allMatch(state -> (state == StageExecutionState.RUNNING) || state.isDone());

        return new QueryStats(
                queryStateTimer.getCreateTimeInMillis(),
                queryStateTimer.getExecutionStartTimeInMillis(),
                queryStateTimer.getLastHeartbeatInMillis(),
                queryStateTimer.getEndTimeInMillis(),

                queryStateTimer.getElapsedTime(),
                queryStateTimer.getWaitingForPrerequisitesTime(),
                queryStateTimer.getQueuedTime(),
                queryStateTimer.getResourceWaitingTime(),
                queryStateTimer.getSemanticAnalyzingTime(),
                queryStateTimer.getColumnAccessPermissionCheckingTime(),
                queryStateTimer.getDispatchingTime(),
                queryStateTimer.getExecutionTime(),
                queryStateTimer.getAnalysisTime(),
                queryStateTimer.getPlanningTime(),
                queryStateTimer.getFinishingTime(),

                totalTasks,
                runningTasks,
                peakRunningTasks,
                completedTasks,

                totalDrivers,
                queuedDrivers,
                runningDrivers,
                blockedDrivers,
                completedDrivers,

                totalNewDrivers,
                queuedNewDrivers,
                runningNewDrivers,
                completedNewDrivers,

                totalSplits,
                queuedSplits,
                runningSplits,
                completedSplits,

                cumulativeUserMemory,
                cumulativeTotalMemory,
                succinctBytes(userMemoryReservation),
                succinctBytes(totalMemoryReservation),
                succinctBytes(peakUserMemoryReservation),
                succinctBytes(peakTotalMemoryReservation),
                succinctBytes(peakTaskUserMemory),
                succinctBytes(peakTaskTotalMemory),
                succinctBytes(peakNodeTotalMemory),

                isScheduled,

                succinctDuration(totalScheduledTime, MILLISECONDS),
                succinctDuration(totalCpuTime, MILLISECONDS),
                succinctDuration(retriedCpuTime, MILLISECONDS),
                succinctDuration(totalBlockedTime, MILLISECONDS),
                fullyBlocked,
                blockedReasons,

                succinctBytes(totalAllocation),

                succinctBytes(rawInputDataSize),
                rawInputPositions,
                succinctBytes(processedInputDataSize),
                processedInputPositions,
                succinctBytes(shuffledDataSize),
                shuffledPositions,
                succinctBytes(outputDataSize),
                outputPositions,

                writtenOutputPositions,
                succinctBytes(writtenOutputLogicalDataSize),
                succinctBytes(writtenOutputPhysicalDataSize),

                succinctBytes(writtenIntermediatePhysicalDataSize),

                stageGcStatistics.build(),

                operatorStatsSummary.build(),
                mergedRuntimeStats);
    }

    private static long computeRetriedCpuTime(StageInfo stageInfo)
    {
        long stageRetriedCpuTime = stageInfo.getPreviousAttemptsExecutionInfos().stream()
                .mapToLong(executionInfo -> executionInfo.getStats().getTotalCpuTime().roundTo(MILLISECONDS))
                .sum();
        long taskRetriedCpuTime = stageInfo.getLatestAttemptExecutionInfo().getStats().getRetriedCpuTime().roundTo(MILLISECONDS);
        return stageRetriedCpuTime + taskRetriedCpuTime;
    }

    public static QueryStats immediateFailureQueryStats()
    {
        long now = System.currentTimeMillis();
        return new QueryStats(
                now,
                now,
                now,
                now,
                new Duration(0, MILLISECONDS),
                new Duration(0, MILLISECONDS),
                new Duration(0, MILLISECONDS),
                new Duration(0, MILLISECONDS),
                new Duration(0, MILLISECONDS),
                new Duration(0, MILLISECONDS),
                new Duration(0, MILLISECONDS),
                new Duration(0, MILLISECONDS),
                new Duration(0, MILLISECONDS),
                new Duration(0, MILLISECONDS),
                new Duration(0, MILLISECONDS),
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                succinctBytes(0),
                succinctBytes(0),
                succinctBytes(0),
                succinctBytes(0),
                succinctBytes(0),
                succinctBytes(0),
                succinctBytes(0),
                false,
                new Duration(0, MILLISECONDS),
                new Duration(0, MILLISECONDS),
                new Duration(0, MILLISECONDS),
                new Duration(0, MILLISECONDS),
                false,
                ImmutableSet.of(),
                succinctBytes(0),
                succinctBytes(0),
                0,
                succinctBytes(0),
                0,
                succinctBytes(0),
                0,
                succinctBytes(0),
                0,
                0,
                succinctBytes(0),
                succinctBytes(0),
                succinctBytes(0),
                ImmutableList.of(),
                ImmutableList.of(),
                new RuntimeStats());
    }

    @JsonProperty
    public DateTime getCreateTime()
    {
        return new DateTime(createTimeInMillis);
    }

    public long getCreateTimeInMillis()
    {
        return createTimeInMillis;
    }

    @JsonProperty
    public DateTime getExecutionStartTime()
    {
        return new DateTime(executionStartTimeInMillis);
    }

    public long getExecutionStartTimeInMillis()
    {
        return executionStartTimeInMillis;
    }

    @JsonProperty
    public DateTime getLastHeartbeat()
    {
        return new DateTime(lastHeartbeatInMillis);
    }

    public long getLastHeartbeatInMillis()
    {
        return lastHeartbeatInMillis;
    }

    @Nullable
    @JsonProperty
    public DateTime getEndTime()
    {
        return new DateTime(endTimeInMillis);
    }

    public long getEndTimeInMillis()
    {
        return endTimeInMillis;
    }

    @JsonProperty
    public Duration getElapsedTime()
    {
        return elapsedTime;
    }

    @JsonProperty
    public Duration getWaitingForPrerequisitesTime()
    {
        return waitingForPrerequisitesTime;
    }

    @JsonProperty
    public Duration getResourceWaitingTime()
    {
        return resourceWaitingTime;
    }

    @JsonProperty
    public Duration getSemanticAnalyzingTime()
    {
        return semanticAnalyzingTime;
    }

    @JsonProperty
    public Duration getColumnAccessPermissionCheckingTime()
    {
        return columnAccessPermissionCheckingTime;
    }

    @JsonProperty
    public Duration getDispatchingTime()
    {
        return dispatchingTime;
    }

    @JsonProperty
    public Duration getQueuedTime()
    {
        return queuedTime;
    }

    @JsonProperty
    public Duration getExecutionTime()
    {
        return executionTime;
    }

    @JsonProperty
    public Duration getAnalysisTime()
    {
        return analysisTime;
    }

    @JsonProperty
    public Duration getTotalPlanningTime()
    {
        return totalPlanningTime;
    }

    @JsonProperty
    public Duration getFinishingTime()
    {
        return finishingTime;
    }

    @JsonProperty
    public int getTotalTasks()
    {
        return totalTasks;
    }

    @JsonProperty
    public int getRunningTasks()
    {
        return runningTasks;
    }

    @JsonProperty
    public int getPeakRunningTasks()
    {
        return peakRunningTasks;
    }

    @JsonProperty
    public int getCompletedTasks()
    {
        return completedTasks;
    }

    @JsonProperty
    public int getTotalDrivers()
    {
        return totalDrivers;
    }

    @JsonProperty
    public int getQueuedDrivers()
    {
        return queuedDrivers;
    }

    @JsonProperty
    public int getRunningDrivers()
    {
        return runningDrivers;
    }

    @JsonProperty
    public int getBlockedDrivers()
    {
        return blockedDrivers;
    }

    @JsonProperty
    public int getCompletedDrivers()
    {
        return completedDrivers;
    }

    @JsonProperty
    public int getTotalNewDrivers()
    {
        return totalNewDrivers;
    }

    @JsonProperty
    public int getQueuedNewDrivers()
    {
        return queuedNewDrivers;
    }

    @JsonProperty
    public int getRunningNewDrivers()
    {
        return runningNewDrivers;
    }

    @JsonProperty
    public int getCompletedNewDrivers()
    {
        return completedNewDrivers;
    }

    @JsonProperty
    public int getTotalSplits()
    {
        return totalSplits;
    }

    @JsonProperty
    public int getQueuedSplits()
    {
        return queuedSplits;
    }

    @JsonProperty
    public int getRunningSplits()
    {
        return runningSplits;
    }

    @JsonProperty
    public int getCompletedSplits()
    {
        return completedSplits;
    }

    @JsonProperty
    public double getCumulativeUserMemory()
    {
        return cumulativeUserMemory;
    }

    @JsonProperty
    public double getCumulativeTotalMemory()
    {
        return cumulativeTotalMemory;
    }

    @JsonProperty
    public DataSize getUserMemoryReservation()
    {
        return userMemoryReservation;
    }

    @JsonProperty
    public DataSize getTotalMemoryReservation()
    {
        return totalMemoryReservation;
    }

    @JsonProperty
    public DataSize getPeakUserMemoryReservation()
    {
        return peakUserMemoryReservation;
    }

    @JsonProperty
    public DataSize getPeakTotalMemoryReservation()
    {
        return peakTotalMemoryReservation;
    }

    @JsonProperty
    public DataSize getPeakTaskTotalMemory()
    {
        return peakTaskTotalMemory;
    }

    @JsonProperty
    public DataSize getPeakNodeTotalMemory()
    {
        return peakNodeTotalMemory;
    }

    @JsonProperty
    public DataSize getPeakTaskUserMemory()
    {
        return peakTaskUserMemory;
    }

    @JsonProperty
    public boolean isScheduled()
    {
        return scheduled;
    }

    @JsonProperty
    public Duration getTotalScheduledTime()
    {
        return totalScheduledTime;
    }

    @JsonProperty
    public Duration getTotalCpuTime()
    {
        return totalCpuTime;
    }

    @JsonProperty
    public Duration getRetriedCpuTime()
    {
        return retriedCpuTime;
    }

    @JsonProperty
    public Duration getTotalBlockedTime()
    {
        return totalBlockedTime;
    }

    @JsonProperty
    public boolean isFullyBlocked()
    {
        return fullyBlocked;
    }

    @JsonProperty
    public Set<BlockedReason> getBlockedReasons()
    {
        return blockedReasons;
    }

    @JsonProperty
    public DataSize getTotalAllocation()
    {
        return totalAllocation;
    }

    @JsonProperty
    public DataSize getRawInputDataSize()
    {
        return rawInputDataSize;
    }

    @JsonProperty
    public long getRawInputPositions()
    {
        return rawInputPositions;
    }

    @JsonProperty
    public DataSize getProcessedInputDataSize()
    {
        return processedInputDataSize;
    }

    @JsonProperty
    public long getProcessedInputPositions()
    {
        return processedInputPositions;
    }

    @JsonProperty
    public DataSize getShuffledDataSize()
    {
        return shuffledDataSize;
    }

    @JsonProperty
    public long getShuffledPositions()
    {
        return shuffledPositions;
    }

    @JsonProperty
    public DataSize getOutputDataSize()
    {
        return outputDataSize;
    }

    @JsonProperty
    public long getOutputPositions()
    {
        return outputPositions;
    }

    @JsonProperty
    public long getWrittenOutputPositions()
    {
        return writtenOutputPositions;
    }

    @JsonProperty
    public DataSize getWrittenOutputLogicalDataSize()
    {
        return writtenOutputLogicalDataSize;
    }

    @JsonProperty
    public DataSize getWrittenOutputPhysicalDataSize()
    {
        return writtenOutputPhysicalDataSize;
    }

    @JsonProperty
    public DataSize getWrittenIntermediatePhysicalDataSize()
    {
        return writtenIntermediatePhysicalDataSize;
    }

    @JsonProperty
    public List<StageGcStatistics> getStageGcStatistics()
    {
        return stageGcStatistics;
    }

    @JsonProperty
    public List<OperatorStats> getOperatorSummaries()
    {
        return operatorSummaries;
    }

    @JsonProperty
    public OptionalDouble getProgressPercentage()
    {
        if (!scheduled || totalDrivers == 0) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(min(100, (completedDrivers * 100.0) / totalDrivers));
    }

    @JsonProperty
    public DataSize getSpilledDataSize()
    {
        return succinctBytes(operatorSummaries.stream()
                .mapToLong(OperatorStats::getSpilledDataSizeInBytes)
                .sum());
    }

    @JsonProperty
    public RuntimeStats getRuntimeStats()
    {
        return runtimeStats;
    }
}
