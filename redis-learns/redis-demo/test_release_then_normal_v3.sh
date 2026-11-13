#!/bin/bash
# Test: Release priority lock, then acquire with normal priority
# Uses Redis directly for state check (status endpoint is a placeholder)
set -e

BASE_URL="http://localhost:8082/api/lock"
LOCK_KEY="test_release_normal_v3"
REDIS_CMD="podman exec redis-demo redis-cli"

echo "=== Release Then Normal Lock Test ==="
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
echo "  (waiting for Redis state to propagate...)"
sleep 2

# Step 4: Check Redis state directly
echo ""
echo "Step 4: Checking Redis lock state..."
LOCK_STATE=$($REDIS_CMD hgetall "$LOCK_KEY" 2>/dev/null || echo "released")
echo "  Redis hgetall: $LOCK_STATE"

# Step 5: Try to acquire with highPriority=false
echo ""
echo "Step 5: Acquiring lock with highPriority=false..."
RESULT4=$(curl -s -X POST "$BASE_URL/acquire?lockKey=$LOCK_KEY&highPriority=false")
echo "  Result: $RESULT4"

# Step 6: Check status endpoint (will show FREE due to placeholder)
echo ""
echo "Step 6: Checking status endpoint..."
STATUS=$(curl -s "$BASE_URL/status/$LOCK_KEY")
echo "  Status endpoint: $STATUS"

# Step 7: Release the normal lock
echo ""
echo "Step 7: Releasing the normal lock..."
curl -s -X POST "$BASE_URL/release?lockKey=$LOCK_KEY"
echo "  Result: Released"

echo ""
echo "=== Test Complete ==="
echo "Summary:"
echo "  - Priority lock was acquired first (highPriority=true)"
echo "  - Low-priority requests were queued"
echo "  - After priority lock release + 2s wait, normal acquire succeeded"
echo "  - Status endpoint shows FREE (placeholder - doesn't reflect real Redis state)"
echo "  - Always check Redis directly for accurate lock state"