#!/bin/bash
# Priority Lock Test Script
# Tests: 100 regular lock requests, then a priority lock request in the middle
# The priority lock should be processed immediately

set -e

BASE_URL="http://localhost:8082/api/lock"
LOCK_KEY="test_priority_lock"

echo "=== Priority Lock Test ==="
echo "Test lock key: $LOCK_KEY"
echo ""

# Step 1: Release any existing lock first
echo "Step 1: Releasing any existing lock..."
curl -s -X POST "$BASE_URL/release?lockKey=$LOCK_KEY" 2>/dev/null || true
sleep 1

# Step 2: Acquire the lock with highPriority=true first (this should be immediate)
echo "Step 2: Acquiring lock with highPriority=true (priority)...."
ACQUIRE_HIGH=$(curl -s -X POST "$BASE_URL/acquire?lockKey=$LOCK_KEY&highPriority=true")
echo "  Result: $ACQUIRE_HIGH"

# Step 3: Now push 100 regular (low priority) acquire requests
echo ""
echo "Step 3: Pushing 100 regular (lowPriority=false) lock requests..."
for i in $(seq 1 100); do
    curl -s -X POST "$BASE_URL/acquire?lockKey=$LOCK_KEY&highPriority=false" > /dev/null &
    # Run a few at a time to avoid overwhelming
    if (( i % 20 == 0 )); then
        wait -n 2>/dev/null || true
    fi
done
wait 2>/dev/null || true

echo "  All 100 low-priority requests submitted."

# Step 4: Try to acquire with highPriority=true again - this should interrupt/preempt
echo ""
echo "Step 4: Acquiring lock with highPriority=true (should preempt)..."
ACQUIRE_HIGH2=$(curl -s -X POST "$BASE_URL/acquire?lockKey=$LOCK_KEY&highPriority=true")
echo "  Result: $ACQUIRE_HIGH2"

# Step 5: Check status
echo ""
echo "Step 5: Checking lock status..."
STATUS=$(curl -s "$BASE_URL/status/$LOCK_KEY")
echo "  Status: $STATUS"

# Step 6: Release the lock
echo ""
echo "Step 6: Releasing lock..."
RELEASE=$(curl -s -X POST "$BASE_URL/release?lockKey=$LOCK_KEY")
echo "  Result: $RELEASE"

echo ""
echo "=== Test Complete ==="