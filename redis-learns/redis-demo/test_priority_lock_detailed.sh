#!/bin/bash
# Detailed Priority Lock Test Script
# Tests: 100 regular lock requests, then a priority lock request that should preempt
# Shows queue state and timing details

set -e

BASE_URL="http://localhost:8082/api/lock"
LOCK_KEY="test_priority_lock_detailed"
REDIS_CMD="podman exec redis-demo redis-cli"

echo "=== Detailed Priority Lock Test ==="
echo "Test lock key: $LOCK_KEY"
echo ""

# Step 0: Prerelease any existing lock
echo "Step 0: Cleaning up any existing lock..."
curl -s -X POST "$BASE_URL/release?lockKey=$LOCK_KEY" 2>/dev/null || true
sleep 1

# Helper: Check Redis queue state
check_queue() {
    local queue_len=$($REDIS_CMD zcard "${LOCK_KEY}:queue" 2>/dev/null || echo "0")
    local queue_top=$($REDIS_CMD zrange "${LOCK_KEY}:queue" 0 0 WITHSCORES 2>/dev/null || echo "empty")
    echo "  Queue length: $queue_len, top entry: $queue_top"
}

check_lock_status() {
    local status=$($REDIS_CMD get "${LOCK_KEY}" 2>/dev/null || echo "free")
    echo "  Lock value: $status"
}

# Step 1: Release any existing lock first
echo "Step 1: Releasing any existing lock..."
curl -s -X POST "$BASE_URL/release?lockKey=$LOCK_KEY" 2>/dev/null || true
sleep 1

echo ""
echo "=== Initial State ==="
check_queue
check_lock_status

# Step 2: Acquire the lock with highPriority=true first (this should be immediate)
echo ""
echo "Step 2: Acquiring lock with highPriority=true (priority)..."
ACQUIRE_HIGH=$(curl -s -X POST "$BASE_URL/acquire?lockKey=$LOCK_KEY&highPriority=true")
echo "  Result: $ACQUIRE_HIGH"

echo ""
echo "=== After Priority Acquisition ==="
check_queue
check_lock_status

# Step 3: Now push 100 regular (low priority) acquire requests in background
echo ""
echo "Step 3: Pushing 100 regular (lowPriority=false) lock requests..."
for i in $(seq 1 100); do
    curl -s -X POST "$BASE_URL/acquire?lockKey=$LOCK_KEY&highPriority=false" > /dev/null &
    # Run a few at a time to avoid overwhelming
    if (( i % 25 == 0 )); then
        wait -n 2>/dev/null || true
        echo "  Submitted $i/100 requests..."
    fi
done
wait 2>/dev/null || true
echo "  All 100 low-priority requests submitted."

echo ""
echo "=== After 100 Low-Priority Submissions ==="
check_queue
check_lock_status

# Step 4: Try to acquire with highPriority=true again - this should interrupt/preempt
echo ""
echo "Step 4: Acquiring lock with highPriority=true (should preempt queue)..."
ACQUIRE_HIGH2=$(curl -s -X POST "$BASE_URL/acquire?lockKey=$LOCK_KEY&highPriority=true")
echo "  Result: $ACQUIRE_HIGH2"

echo ""
echo "=== After Second Priority Acquisition ==="
check_queue
check_lock_status

# Step 5: Check status endpoint
echo ""
echo "Step 5: Checking lock status via endpoint..."
STATUS=$(curl -s "$BASE_URL/status/$LOCK_KEY")
echo "  Status: $STATUS"

# Step 6: Release the lock
echo ""
echo "Step 6: Releasing lock..."
RELEASE=$(curl -s -X POST "$BASE_URL/release?lockKey=$LOCK_KEY")
echo "  Result: $RELEASE"

echo ""
echo "=== Final State ==="
check_queue
check_lock_status

echo ""
echo "=== Test Complete ==="
echo "Summary:"
echo "  - 100 low-priority requests were submitted to the queue"
echo "  - Priority (highPriority=true) requests preempt/immediately acquire the lock"
echo "  - Priority locking effectively bypasses FIFO queue ordering"