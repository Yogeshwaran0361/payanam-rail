package com.payanam.rail.domain.engine;

import java.util.List;

/** Generic result of a station operation: a summary message plus the follow-up allocations it caused. */
public record OperationResult(String message, List<AllocationResult> allocations, List<String> alerts) {
}
