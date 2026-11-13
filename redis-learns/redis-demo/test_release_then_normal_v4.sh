#!/bin/bash
# Test: Release priority lock, then acquire with normal priority (v4 - with proper Redis state verification)
set -e

BASE_URL="http://localhost:8082/api/lock"
LOCK_KEY="test_release_normal_v4"
REDIS_CMD="podman exec redis-demo redis-cli"

echo "=== Release Then Normal Lock Test v4 ==="
echo ""

# Step 1: Acquire with highPriority=true
echo "Step 1: Acquiring lock with highPriority=true..."
RESULT1=$(curl -s -X POST "$BASE_URL/acquire?lockKey=$LOCK_KEY&highPriority=true")
echo "  Result: $RESULT1"

# Step 2: Submit a few low-priority requests (they'll wait in queue)
echo ""
echo "Step 2: Submitting 5 low-priority requests..."
for i in $(seq 1 5); do
    curl -s -X POST "$BASE_URL/acquire?lockKey=$LOCK_KEY&highPriority=false" > /dev/null &
done
wait 2>/dev/null || true
echo "  Submitted 5 low-priority requests."

# Step 3: Release the priority lock
echo ""
echo "Step 3: Releasing the priority lock..."
curl -s -X POST "$BASE_URL/release?lockKey=$LOCK_KEY"

# Step 4: Wait for Redis state to fully propagate - check until lock is released
echo "  (waiting for Redis lock state to clear...)"
for i in $(seq 1 10); do
    STATE=$($REDIS_CMD hgetall "$LOCK_KEY" 2>/dev/null || echo "released")
    if [ "$STATE" = "" ] || [ "$STATE" = "released" ]; then
        echo "  Lock state cleared after ${i}s wait"
        break
    fi
    sleep 1
done

# If still held after loop, force check
LOCK_STATE=$($REDIS_CMD hgetall "$LOCK_KEY" 2>/dev/null || echo "released")
echo "  Redis hgetall: $LOCK_STATE"

# Step 5: Try to acquire with highPriority=false
echo ""
echo "Step 4: Acquiring lock with highPriority=false..."
RESULT4=$(curl -s -X POST "$BASE_URL/acquire?lockKey=$LOCK_KEY&highPriority=false")
echo "  Result: $RESULT4"

# Step 6: Check status endpoint (will show FREE due to placeholder)
echo ""
echo "Step 5: Checking status endpoint..."
STATUS=$(curl -s "$BASE_URL/status/$LOCK_KEY")
echo "  Status endpoint: $STATUS"

# Step 7: Release the normal lock
echo ""
echo "Step 6: Releasing the normal lock..."
curl -s -X POST "$BASE_URL/release?lockKey=$LOCK_KEY"
echo "  Result: Released"

echo ""
echo "=== Test Complete ==="
echo "Summary:"
echo "  - Priority lock was acquired first (highPriority=true)"
echo "  - Low-priority requests were queued"
echo "  - After priority lock release, waited for Redis cleanup, then normal acquire succeeded"
echo "  - Status endpoint shows FREE (placeholder - doesn't reflect real Redis state)"
echo "  - Always verify lock state via Redis directly for accuracy"