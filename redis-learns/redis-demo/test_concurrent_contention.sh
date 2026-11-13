#!/bin/bash
# Concurrent Contention Test Script
# Simulates multiple clients competing for locks under load
# Tests both highPriority and highPriority=false scenarios

set -e

BASE_URL="http://localhost:8085/api/lock"
CONTEST_KEY="contention_load"
CLIENTS=10
REQUESTS_PER_CLIENT=5

echo "=== Concurrent Contention Test ==="
echo "Clients: $CLIENTS"
echo "Requests per client: $REQUESTS_PER_CLIENT"
echo "Lock key: $CONTEST_KEY"
echo ""

# Step 1: Clean state
echo "Step 1: Cleaning Redis state..."
podman exec redis-demo redis-cli FLUSHALL 2>/dev/null || true
sleep 1

# Step 2: Acquire priority lock (one client)
echo ""
echo "Step 2: One client acquires highPriority=true lock..."
HIGH_RESULT=$(curl -s -m 3 -X POST "$BASE_URL/acquire?lockKey=$CONTEST_KEY&highPriority=true")
echo "  Result: $HIGH_RESULT"

# Step 3: Submit concurrent requests from multiple "clients"
echo ""
echo "Step 3: Submitting $CLIENTS clients × $REQUESTS_PER_CLIENT concurrent requests..."

# Track results per client
declare -a HIGH_RESULTS
declare -a NORMAL_RESULTS
SUCCESS_COUNT=0
WAIT_COUNT=0

for client in $(seq 1 $CLIENTS); do
    for req in $(seq 1 $REQUESTS_PER_CLIENT); do
        # Submit highPriority requests
        HIGH_CURL=$(curl -s -m 3 -X POST "$BASE_URL/acquire?lockKey=$CONTEST_KEY&highPriority=true" 2>/dev/null || echo "ERROR")
        if [[ "$HIGH_CURL" == "Lock acquired:"* ]]; then
            SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
        else
            WAIT_COUNT=$((WAIT_COUNT + 1))
        fi
        HIGH_RESULTS+=("$HIGH_CURL")

        # Submit normal priority requests (these will queue/wait)
        NORMAL_CURL=$(curl -s -m 10 -X POST "$BASE_URL/acquire?lockKey=$CONTEST_KEY&highPriority=false" 2>/dev/null || echo "ERROR")
        NORMAL_RESULTS+=("$NORMAL_CURL")
    done
    # Small delay between clients to simulate real concurrency
    sleep 0.2
done

# Wait for all background curl processes to complete
wait 2>/dev/null || true

# Step 4: Summary
echo ""
echo "=== Results Summary ==="
echo "Total requests submitted: $((CLIENTS * REQUESTS_PER_CLIENT * 2))"
echo "HighPriority requests: $((CLIENTS * REQUESTS_PER_CLIENT))"
echo "NormalPriority requests: $((CLIENTS * REQUESTS_PER_CLIENT))"
echo ""
echo "HighPriority results:"
for r in "${HIGH_RESULTS[@]}"; do
    echo "  $r"
done
echo ""
echo "NormalPriority results:"
for r in "${NORMAL_RESULTS[@]}"; do
    echo "  $r"
done
echo ""

# Step 5: Check final lock status
echo "Step 4: Final lock status..."
STATUS=$(curl -s "http://localhost:8085/api/lock/status/$CONTEST_KEY")
echo "  $STATUS"

# Step 6: Release lock
echo ""
echo "Step 5: Releasing lock..."
curl -s -X POST "$BASE_URL/release?lockKey=$CONTEST_KEY" 2>/dev/null || true
sleep 1

echo ""
echo "=== Contention Test Complete ==="