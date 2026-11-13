#!/bin/bash
# Priority Lock Benchmark Script
# Measures: time to acquire lock with highPriority=true vs highPriority=false
# under queued conditions

set -e

BASE_URL="http://localhost:8084/api/lock"
LOCK_KEY="test_benchmark"
ITERATIONS=10

echo "=== Priority Lock Benchmark ==="
echo "Iterations: $ITERATIONS"
echo "Lock key: $LOCK_KEY"
echo ""

# Step 1: Clean state
echo "Step 1: Cleaning state..."
curl -s -X POST "$BASE_URL/release?lockKey=$LOCK_KEY" 2>/dev/null || true
sleep 1

# Step 2: Acquire with highPriority=true (should be immediate)
echo ""
echo "Step 2: Measuring highPriority=true acquisition time ($ITERATIONS iterations)..."
HIGH_TIMES=()
for i in $(seq 1 $ITERATIONS); do
    START=$(date +%s%N)
    RESULT=$(curl -s -o /dev/null -w "%{time_total}" -X POST "$BASE_URL/acquire?lockKey=$LOCK_KEY&highPriority=true")
    END=$(date +%s%N)
    ELAPSED=$(( (END - START) / 1000000 ))  # milliseconds
    HIGH_TIMES+=($ELAPSED)
    echo "  Iteration $i: ${ELAPSED}ms - $RESULT"
done

# Step 3: Submit queued low-priority requests first
echo ""
echo "Step 3: Submitting $ITERATIONS low-priority requests to queue..."
for i in $(seq 1 $ITERATIONS); do
    curl -s -X POST "$BASE_URL/acquire?lockKey=$LOCK_KEY&highPriority=false" > /dev/null &
    if (( i % 5 == 0 )); then
        wait -n 2>/dev/null || true
    fi
done
wait 2>/dev/null || true
echo "  All $ITERATIONS low-priority requests submitted."

# Step 4: Measure normal acquire time (should wait in queue)
echo ""
echo "Step 4: Measuring highPriority=false acquisition time ($ITERATIONS iterations)..."
NORMAL_TIMES=()
for i in $(seq 1 $ITERATIONS); do
    START=$(date +%s%N)
    RESULT=$(curl -s -o /dev/null -w "%{time_total}" -X POST "$BASE_URL/acquire?lockKey=$LOCK_KEY&highPriority=false")
    END=$(date +%s%N)
    ELAPSED=$(( (END - START) / 1000000 ))  # milliseconds
    NORMAL_TIMES+=($ELAPSED)
    echo "  Iteration $i: ${ELAPSED}ms - $RESULT"
done

# Step 5: Summary
echo ""
echo "=== Benchmark Results ==="
echo "highPriority=true times (ms):"
for t in "${HIGH_TIMES[@]}"; do
    echo "  $t"
done
HIGH_AVG=$(echo ${HIGH_TIMES[@]} | tr ' ' '+' | bc -l | xargs printf "%.2f")
echo "Average highPriority=true: ${HIGH_AVG}ms"

echo "highPriority=false times (ms):"
for t in "${NORMAL_TIMES[@]}"; do
    echo "  $t"
done
NORMAL_AVG=$(echo ${NORMAL_TIMES[@]} | tr ' ' '+' | bc -l | xargs printf "%.2f")
echo "Average highPriority=false: ${NORMAL_AVG}ms"

if (( $(echo "$NORMAL_AVG > $HIGH_AVG" | bc -l) )); then
    SPEEDUP=$(echo "scale=2; $NORMAL_AVG / $HIGH_AVG" | bc -l)
    echo "Speedup factor (normal/priority): ~${SPEEDUP}x (priority is ~${SPEEDUP} times faster)"
else
    echo "Priority not significantly faster in this run"
fi

# Step 6: Release all locks
echo ""
echo "Step 5: Releasing all locks..."
for i in $(seq 1 $ITERATIONS); do
    curl -s -X POST "$BASE_URL/release?lockKey=$LOCK_KEY" 2>/dev/null || true
done
sleep 1

echo ""
echo "=== Benchmark Complete ==="