#!/bin/bash
# Test: Release priority lock, then acquire with normal priority
set -e

BASE_URL="http://localhost:8082/api/lock"
LOCK_KEY="test_release_normal"

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
sleep 1

# Step 4: Try to acquire with highPriority=false - should now succeed
echo ""
echo "Step 4: Acquiring lock with highPriority=false (should succeed now)..."
RESULT4=$(curl -s -X POST "$BASE_URL/acquire?lockKey=$LOCK_KEY&highPriority=false")
echo "  Result: $RESULT4"

# Step 5: Check status
echo ""
echo "Step 5: Checking lock status..."
STATUS=$(curl -s "$BASE_URL/status/$LOCK_KEY")
echo "  Status: $STATUS"

# Step 6: Release the normal lock
echo ""
echo "Step 6: Releasing the normal lock..."
curl -s -X POST "$BASE_URL/release?lockKey=$LOCK_KEY"
echo "  Result: Released"

echo ""
echo "=== Test Complete ==="
